package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceCategory;

import java.util.List;
import java.util.Optional;

public interface ResourceDao {
    int insert(Resource resource);
    boolean updateStatus(int resourceId, String status);
    boolean delete(int resourceId);
    Optional<Resource> findById(int resourceId);
    Optional<Resource> findByCode(String code);
    List<Resource> findAll();
    List<Resource> findByStatus(String status);
    List<ResourceCategory> findCategories();
    Optional<ResourceCategory> findCategoryById(int categoryId);
}
