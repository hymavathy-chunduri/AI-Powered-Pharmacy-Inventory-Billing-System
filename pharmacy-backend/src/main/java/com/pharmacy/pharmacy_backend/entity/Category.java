package com.pharmacy.pharmacy_backend.entity;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "categories")
public class Category {

    @Id
    private String id;

    @NotBlank(message = "Category name is required")
    @Indexed(unique = true)
    @Field("category_name")
    private String categoryName;

    @Field("description")
    private String description;

    public Category() {}

    public Category(String id, String categoryName, String description) {
        this.id = id;
        this.categoryName = categoryName;
        this.description = description;
    }

    // Keep old getter name for frontend compatibility
    public String getCategoryId() {
        return id;
    }

    public void setCategoryId(String categoryId) {
        this.id = categoryId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
