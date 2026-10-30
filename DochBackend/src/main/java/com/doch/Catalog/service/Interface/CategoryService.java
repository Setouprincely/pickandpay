package com.doch.Catalog.service.Interface;


import java.util.List;

import com.doch.Catalog.dto.request.CategoryRequest;
import com.doch.Catalog.model.Category;

public interface CategoryService {

    String createCategory(CategoryRequest request);
    Category updateCategory (CategoryRequest request, String id);
    void deleteCategory(String id);
    List<Category> getAllCategories();
    Category getCatedoryById(String id);

}
