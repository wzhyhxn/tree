package com.campusplant.dto;

import java.util.List;

public class PlantVO {
    private String id;
    private String name;          // 樱花-01
    private String category;      // 樱花
    private String description;
    private String locationName;  // 图书馆东侧
    private Double longitude;
    private Double latitude;
    private String image;         // 专属图片
    private String fallbackImage; // 同类型随机备选图片
    private List<String> images;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getFallbackImage() { return fallbackImage; }
    public void setFallbackImage(String fallbackImage) { this.fallbackImage = fallbackImage; }
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
}
