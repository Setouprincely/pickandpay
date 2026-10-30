package com.doch.Catalog.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.doch.Catalog.dto.request.CategoryRequest;
import com.doch.Catalog.model.Category;
import com.doch.Catalog.service.Interface.CategoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(
            summary = "Category Creation",
            description = "Create the category of the products.",
            security = @SecurityRequirement(name = "bearerAuth"),
            tags = {"Category"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category Created successful",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid credentials",
                    content = @Content(mediaType = "application/json"))
    })

    @PostMapping("/create")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyAuthority('MANAGE_MARKET', 'MANAGE_SYSTEM')")
    public String createCategory(@RequestBody CategoryRequest request){
        if (request.getName() == null || request.getName().isBlank()) {
            throw new com.doch.common.exceptions.BusinessException("Category name is required.");
        }
        return categoryService.createCategory(request);
    }

    @GetMapping("/{categoryId}")
    public Category getCategory(@PathVariable String categoryId){
        return categoryService.getCatedoryById(categoryId);
    }

    @PutMapping("/{categoryId}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyAuthority('MANAGE_MARKET', 'MANAGE_SYSTEM')")
    public Category updateCategory(@PathVariable String categoryId, @RequestBody CategoryRequest request){
        return categoryService.updateCategory(request, categoryId);
    }

    @GetMapping()
    public List<Category> getAllCategories(){
        return categoryService.getAllCategories();
    }

    @Operation(
            summary = "Category Deletion",
            description = "Delete an existing category.",
            security = @SecurityRequirement(name = "bearerAuth"),
            tags = {"Category"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category Deleted successful"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid credentials",
                    content = @Content(mediaType = "application/json"))
    })
    @DeleteMapping("/{categoryId}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyAuthority('MANAGE_MARKET', 'MANAGE_SYSTEM')")
    public void deleteCategory(@PathVariable String categoryId){
        categoryService.deleteCategory(categoryId);
    }

}
