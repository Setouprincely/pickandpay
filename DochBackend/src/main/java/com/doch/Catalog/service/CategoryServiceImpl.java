package com.doch.Catalog.service;


import java.util.List;

import org.springframework.stereotype.Service;

import com.doch.Catalog.dto.request.CategoryRequest;
import com.doch.Catalog.model.Category;
import com.doch.Catalog.repository.CategoryRepository;
import com.doch.Catalog.service.Interface.CategoryService;
import com.doch.common.exceptions.BusinessException;
import com.doch.common.exceptions.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService{

    private final CategoryRepository categoryRepository;

    @Override
    public String createCategory(CategoryRequest request) {
        Category categoryExist = categoryRepository.findByName(request.getName());
        if (categoryExist != null) {
            throw new BusinessException("A category with this name already exists.");
        }
        Category category = Category.builder().description(request.getDescription())
        .name(request.getName()).build();
        category = categoryRepository.save(category);
        return category.getId();
    }

    @Override
    public Category updateCategory(CategoryRequest request, String id) {
        Category category = categoryRepository.findById(id).orElseThrow(
            () -> new ResourceNotFoundException("Category not found: " + id)
        );

        Category categoryWithName = categoryRepository.findByName(request.getName());
        if (categoryWithName != null && !categoryWithName.getId().equals(id)) {
            throw new BusinessException("A category with this name already exists.");
        }

        category.setDescription(request.getDescription());
        category.setName(request.getName());
        return categoryRepository.save(category);
    }

    @Override
    public void deleteCategory(String id) {
       Category category = categoryRepository.findById(id).orElseThrow(
            () -> new ResourceNotFoundException("Category not found: " + id)
        );
        category.setIsDeleted(true);
        categoryRepository.save(category);
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public Category getCatedoryById(String id) {
        return categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
       
    }

}
