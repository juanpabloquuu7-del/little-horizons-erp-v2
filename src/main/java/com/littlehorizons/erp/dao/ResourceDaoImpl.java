package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceCategory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class ResourceDaoImpl extends BaseJdbcDao implements ResourceDao {

    private static final String SELECT = """
            SELECT r.id_recurso, r.codigo_recurso, r.nombre_recurso, r.id_categoria, c.nombre_categoria, r.descripcion,
                   r.fecha_adquisicion, r.ubicacion_fisica, r.estado_recurso
            FROM recursos r JOIN categorias_recurso c ON c.id_categoria = r.id_categoria
            """;

    private static Resource map(ResultSet rs) throws SQLException {
        return new Resource(rs.getInt("id_recurso"), rs.getString("codigo_recurso"), rs.getString("nombre_recurso"),
                rs.getInt("id_categoria"), rs.getString("nombre_categoria"), rs.getString("descripcion"),
                rs.getObject("fecha_adquisicion", LocalDate.class), rs.getString("ubicacion_fisica"), rs.getString("estado_recurso"));
    }

    @Override
    public int insert(Resource r) {
        return insert("INSERT INTO recursos (codigo_recurso, nombre_recurso, id_categoria, descripcion, fecha_adquisicion, ubicacion_fisica, estado_recurso) VALUES (?, ?, ?, ?, ?, ?, ?)",
                r.code(), r.name(), r.categoryId(), nullIfBlank(r.description()), r.acquisitionDate(),
                nullIfBlank(r.location()), r.status());
    }

    @Override
    public boolean updateStatus(int resourceId, String status) {
        return update("UPDATE recursos SET estado_recurso = ? WHERE id_recurso = ?", status, resourceId) > 0;
    }

    @Override
    public boolean delete(int resourceId) {
        return update("DELETE FROM recursos WHERE id_recurso = ?", resourceId) > 0;
    }

    @Override
    public Optional<Resource> findById(int resourceId) {
        return queryOne(SELECT + " WHERE r.id_recurso = ?", ResourceDaoImpl::map, resourceId);
    }

    @Override
    public Optional<Resource> findByCode(String code) {
        return queryOne(SELECT + " WHERE r.codigo_recurso = ?", ResourceDaoImpl::map, code);
    }

    @Override
    public List<Resource> findAll() {
        return query(SELECT + " ORDER BY r.codigo_recurso", ResourceDaoImpl::map);
    }

    @Override
    public List<Resource> findByStatus(String status) {
        return query(SELECT + " WHERE r.estado_recurso = ? ORDER BY r.codigo_recurso", ResourceDaoImpl::map, status);
    }

    @Override
    public List<ResourceCategory> findCategories() {
        return query("SELECT id_categoria, nombre_categoria, descripcion FROM categorias_recurso ORDER BY id_categoria",
                rs -> new ResourceCategory(rs.getInt(1), rs.getString(2), rs.getString(3)));
    }

    @Override
    public Optional<ResourceCategory> findCategoryById(int categoryId) {
        return queryOne("SELECT id_categoria, nombre_categoria, descripcion FROM categorias_recurso WHERE id_categoria = ?",
                rs -> new ResourceCategory(rs.getInt(1), rs.getString(2), rs.getString(3)), categoryId);
    }
}
