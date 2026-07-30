package com.campusplant.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "plant_season")
public class PlantSeason {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false, length = 4)
    private String season;

    public PlantSeason() {}
    public PlantSeason(String category, String season) { this.category = category; this.season = season; }

    public Integer getId() { return id; }
    public String getCategory() { return category; }
    public String getSeason() { return season; }
    public void setId(Integer id) { this.id = id; }
    public void setCategory(String c) { this.category = c; }
    public void setSeason(String s) { this.season = s; }
}
