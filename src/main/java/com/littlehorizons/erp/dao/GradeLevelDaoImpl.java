package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.GradeLevel;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class GradeLevelDaoImpl extends BaseJdbcDao implements GradeLevelDao {

    private static final String SELECT = "SELECT id_grado, nombre_grado, nivel_numerico FROM grados";

    private static GradeLevel map(ResultSet rs) throws SQLException {
        return new GradeLevel(rs.getInt("id_grado"), rs.getString("nombre_grado"), rs.getInt("nivel_numerico"));
    }

    @Override
    public List<GradeLevel> findAll() {
        return query(SELECT + " ORDER BY nivel_numerico", GradeLevelDaoImpl::map);
    }

    @Override
    public Optional<GradeLevel> findById(int id) {
        return queryOne(SELECT + " WHERE id_grado = ?", GradeLevelDaoImpl::map, id);
    }
}
