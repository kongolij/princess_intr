package com.bigcommerce.imports.catalog.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CategoryNode {

	private String id;
    private String name;
    private Map<String,String> localizedName;
    private String parentId;
    private String description;
    private Map<String,String> localizedDescription;
    private String slug;
    private List<CategoryNode> children = new ArrayList<>();
    private List<String> childrenIds = new ArrayList<>();
    private boolean active;
    private String imageFileName;

    // Constructor, getters, and setters
    public CategoryNode(String id, String name, String parentId, String description, String slug) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.description=description;
        this.slug=slug;
    }
    
	public CategoryNode(String id, String name, Map<String, String> localizedName, String parentId, String description,
			Map<String, String> localizedDescription, String slug) {
		this.id = id;
		this.name = name;
		this.localizedName = localizedName;
		this.parentId = parentId;
		this.description = description;
		this.localizedDescription = localizedDescription;
		this.slug = slug;
	}
	
	public CategoryNode(String id, String name, Map<String, String> localizedName, String parentId, String description,
			Map<String, String> localizedDescription, String slug, List<String> childrenIds) {
		this.id = id;
		this.name = name;
		this.localizedName = localizedName;
		this.parentId = parentId;
		this.description = description;
		this.localizedDescription = localizedDescription;
		this.slug = slug;
		this.setChildrenIds(childrenIds);
	}
	
	public CategoryNode(String id, String name, Map<String, String> localizedName, String parentId, String description,
			Map<String, String> localizedDescription, String slug, String type) {
		this.id = id;
		this.name = name;
		this.localizedName = localizedName;
		this.parentId = parentId;
		this.description = description;
		this.localizedDescription = localizedDescription;
		this.slug = slug;
	}
	
    
    public CategoryNode(String id, String name, String parentId) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getParentId() {
        return parentId;
    }

    
    public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public List<CategoryNode> getChildren() {
        return children;
    }

    public void addChild(CategoryNode child) {
        children.add(child);
    }

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	public Map<String, String> getLocalizedName() {
		return localizedName;
	}

	public void setLocalizedName(Map<String, String> localizedName) {
		this.localizedName = localizedName;
	}

	public Map<String, String> getLocalizedDescription() {
		return localizedDescription;
	}

	public void setLocalizedDescription(Map<String, String> localizedDescription) {
		this.localizedDescription = localizedDescription;
	}

	public List<String> getChildrenIds() {
		return childrenIds;
	}

	public void setChildrenIds(List<String> childrenIds) {
		this.childrenIds = childrenIds;
	}
	
	public List<String> getChildSkList() {
	    return childrenIds;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getImageFileName() {
		return imageFileName;
	}

	public void setImageFileName(String imageFileName) {
		this.imageFileName = imageFileName;
	}
	
	
	
    
}
