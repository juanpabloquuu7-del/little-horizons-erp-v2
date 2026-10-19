package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.config.DatabaseConnection;
import com.littlehorizons.erp.util.CustomExceptions.DataAccessException;
import com.littlehorizons.erp.util.LoggerUtility;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Shared JDBC plumbing for all DAOs. Every statement is a PreparedStatement (user values are always
 * bound with '?') and every resource is closed with try-with-resources. SQL errors are logged and
 * translated into {@link DataAccessException}.
 */
abstract class BaseJdbcDao {

    /** Fixed SQL fragments (no user input) used to build display names. Aliases: e, p, a. */
    protected static final String STUDENT_NAME =
            "CONCAT_WS(' ', e.primer_nombre, e.segundo_nombre, e.primer_apellido, e.segundo_apellido)";
    protected static final String TEACHER_NAME =
            "CONCAT_WS(' ', p.primer_nombre, p.segundo_nombre, p.primer_apellido, p.segundo_apellido)";

    /** Maps the current row of a ResultSet to an object. */
    @FunctionalInterface
    protected interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    /** Maps every row to a String[] with the given number of columns (display reports). */
    protected static RowMapper<String[]> columns(int count) {
        return rs -> {
            String[] row = new String[count];
            for (int i = 0; i < count; i++) {
                row[i] = rs.getString(i + 1);
            }
            return row;
        };
    }

    protected Connection openConnection() {
        return DatabaseConnection.getInstance().getConnection();
    }

    protected <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapper.map(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw translate(sql, e);
        }
    }

    protected <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... params) {
        return query(sql, mapper, params).stream().findFirst();
    }

    protected int count(String sql, Object... params) {
        return queryOne(sql, rs -> rs.getInt(1), params).orElse(0);
    }

    /** Executes INSERT/UPDATE/DELETE and returns the number of affected rows. */
    protected int update(String sql, Object... params) {
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw translate(sql, e);
        }
    }

    /** Executes an INSERT and returns the generated key. */
    protected int insert(String sql, Object... params) {
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, params);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw translate(sql, e);
        }
    }

    /** Binds parameters to a PreparedStatement, converting java.time types. */
    protected static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            int index = i + 1;
            if (p == null) {
                ps.setNull(index, Types.NULL);
            } else if (p instanceof LocalDate d) {
                ps.setDate(index, Date.valueOf(d));
            } else if (p instanceof LocalDateTime dt) {
                ps.setTimestamp(index, Timestamp.valueOf(dt));
            } else if (p instanceof LocalTime t) {
                ps.setTime(index, Time.valueOf(t));
            } else {
                ps.setObject(index, p);
            }
        }
    }

    /** Converts a 0 id (meaning "none") to null for nullable foreign keys. */
    protected static Integer nullableId(int id) {
        return id == 0 ? null : id;
    }

    /** Converts blank text to null for optional columns. */
    protected static String nullIfBlank(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    protected DataAccessException translate(String sql, SQLException e) {
        LoggerUtility.logError(getClass().getSimpleName() + " [" + sql + "]", e);
        if (e instanceof SQLIntegrityConstraintViolationException) {
            return new DataAccessException("La operación viola una restricción de integridad "
                    + "(dato duplicado, inexistente o con registros relacionados).", e);
        }
        return new DataAccessException("Error al acceder a la base de datos.", e);
    }
}
