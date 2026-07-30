package com.campusplant.controller;

import com.campusplant.config.TokenHolder;
import com.campusplant.dto.PlantVO;
import com.campusplant.dto.Result;
import com.campusplant.entity.PlantSeason;
import com.campusplant.repository.PlantSeasonRepository;
import com.campusplant.service.PlantService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api")
public class PlantController {

    private final PlantService plantService;
    private final PlantSeasonRepository seasonRepo;

    public PlantController(PlantService plantService, PlantSeasonRepository seasonRepo) {
        this.plantService = plantService;
        this.seasonRepo = seasonRepo;
    }

    // ========= 公共接口 =========

    @GetMapping("/plants")
    public Result<List<PlantVO>> list(@RequestParam(required = false) String search,
                                      @RequestParam(required = false) String season) {
        return Result.ok(plantService.list(search, season));
    }

    @GetMapping("/plants/{id}")
    public Result<PlantVO> detail(@PathVariable String id) {
        return Result.ok(plantService.detail(id));
    }

    @GetMapping("/seasons")
    public Result<List<PlantSeason>> seasons() {
        return Result.ok(seasonRepo.findAll());
    }

    // ========= 认证 =========

    @PostMapping("/auth/login")
    public Result<String> login(@RequestBody Map<String, String> body) {
        if ("root".equals(body.get("username")) && "root".equals(body.get("password"))) {
            String token = UUID.randomUUID().toString();
            TokenHolder.add(token);
            return Result.ok(token);
        }
        throw new RuntimeException("用户名或密码错误");
    }

    // ========= 管理接口 =========

    @PostMapping("/admin/plants")
    public Result<PlantVO> create(@RequestParam("category") String category,
            @RequestParam("description") String description,
            @RequestParam("name") String name,
            @RequestParam("longitude") Double longitude,
            @RequestParam("latitude") Double latitude,
            @RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
        return Result.ok(plantService.create(name, category, description, longitude, latitude, file));
    }

    @PostMapping("/admin/plants/{id}/images")
    public Result<String> uploadImage(@PathVariable String id, @RequestParam("file") MultipartFile file) throws IOException {
        return Result.ok(plantService.uploadImage(id, file));
    }

    @DeleteMapping("/admin/plants/{id}")
    public Result<String> delete(@PathVariable String id) {
        plantService.delete(id);
        return Result.ok("ok");
    }

    @PutMapping("/admin/plants/{id}")
    public Result<PlantVO> update(@PathVariable String id,
            @RequestParam("category") String category,
            @RequestParam("description") String description,
            @RequestParam("name") String name,
            @RequestParam("longitude") Double longitude,
            @RequestParam("latitude") Double latitude,
            @RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
        return Result.ok(plantService.update(id, category, description, name, longitude, latitude, file));
    }

    /** 管理端保存季节关系（全量替换） */
    @PutMapping("/admin/seasons")
    @Transactional
    public Result<String> setSeasons(@RequestBody List<Map<String, String>> mappings) {
        seasonRepo.deleteAllInBatch();
        for (Map<String, String> m : mappings) {
            seasonRepo.save(new PlantSeason(m.get("category"), m.get("season")));
        }
        return Result.ok("ok");
    }
}
