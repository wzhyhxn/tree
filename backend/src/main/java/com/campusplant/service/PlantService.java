package com.campusplant.service;

import com.campusplant.dto.PlantVO;
import com.campusplant.entity.*;
import com.campusplant.repository.*;
import com.campusplant.storage.StorageService;
import org.locationtech.jts.geom.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PlantService {

    private final PlantSpeciesRepository speciesRepo;
    private final PlantLocationRepository locationRepo;
    private final PlantImageRepository imageRepo;
    private final PlantSeasonRepository seasonRepo;
    private final StorageService storage;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    private final Random random = new Random();

    public PlantService(PlantSpeciesRepository speciesRepo, PlantLocationRepository locationRepo,
                        PlantImageRepository imageRepo, PlantSeasonRepository seasonRepo,
                        StorageService storage) {
        this.speciesRepo = speciesRepo;
        this.locationRepo = locationRepo;
        this.imageRepo = imageRepo;
        this.seasonRepo = seasonRepo;
        this.storage = storage;
    }

    /** 植物列表，支持搜索和季节筛选 */
    public List<PlantVO> list(String search, String season) {
        List<PlantLocation> locations = locationRepo.findAll();

        if (search != null && !search.isEmpty()) {
            Set<String> ids = speciesRepo.findByNameContaining(search).stream()
                    .map(PlantSpecies::getId).collect(Collectors.toSet());
            locations = locations.stream().filter(l -> ids.contains(l.getSpecies().getId())).toList();
        }

        if (season != null && !season.isEmpty()) {
            Set<String> cats = seasonRepo.findBySeason(season).stream()
                    .map(PlantSeason::getCategory).collect(Collectors.toSet());
            locations = locations.stream()
                    .filter(l -> cats.contains(l.getSpecies().getCategory())).toList();
        }

        return toVOList(locations);
    }

    /** 详情 */
    public PlantVO detail(String id) {
        PlantSpecies sp = speciesRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("树木不存在"));
        PlantLocation loc = locationRepo.findAll().stream()
                .filter(l -> l.getSpecies().getId().equals(id))
                .findFirst().orElse(null);

        return toVO(sp, loc);
    }

    /** 自动生成编号：类型-序号 */
    @Transactional
    public PlantVO create(String name, String category, String description,
                           Double longitude, Double latitude, MultipartFile file) throws IOException {
        // 编号：只统计同类型已有数量
        List<PlantSpecies> sameCategory = speciesRepo.findAll().stream()
                .filter(s -> s.getCategory().equals(category)).toList();
        String code = category + "-" + String.format("%02d", sameCategory.size() + 1);

        // 简介：若未填写，复用同类型已有的
        if (description == null || description.isBlank()) {
            description = sameCategory.stream()
                    .map(PlantSpecies::getDescription)
                    .filter(d -> d != null && !d.isBlank())
                    .findFirst().orElse("");
        }

        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        PlantSpecies sp = new PlantSpecies();
        sp.setId(id);
        sp.setName(code);
        sp.setCategory(category);
        sp.setDescription(description);
        speciesRepo.save(sp);

        PlantLocation loc = new PlantLocation();
        loc.setSpecies(sp);
        loc.setName(name);
        loc.setLongitude(longitude);
        loc.setLatitude(latitude);
        Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude));
        loc.setGeom(point);

        if (file != null && !file.isEmpty()) {
            String filename = "plants/" + UUID.randomUUID() + "_" + sanitizeFilename(file.getOriginalFilename());
            String url = storage.upload(filename, file.getInputStream());
            loc.setImage(url);
            // 同时入库到图片库
            PlantImage img = new PlantImage();
            img.setCategory(category);
            img.setImageUrl(url);
            imageRepo.save(img);
        }

        locationRepo.save(loc);
        return detail(id);
    }

    /** 删除树木 */
    @Transactional
    public void delete(String id) {
        locationRepo.deleteBySpeciesId(id);
        speciesRepo.deleteById(id);
    }

    /** 修改树木 */
    @Transactional
    public PlantVO update(String id, String category, String description,
                           String name, Double longitude, Double latitude,
                           MultipartFile file) throws IOException {
        PlantSpecies sp = speciesRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("树木不存在"));

        // 如果类型变化，更新编号
        if (!category.equals(sp.getCategory())) {
            List<PlantSpecies> sameCategory = speciesRepo.findAll().stream()
                    .filter(s -> s.getCategory().equals(category)).toList();
            String newCode = category + "-" + String.format("%02d", sameCategory.size() + 1);
            sp.setName(newCode);
        }
        sp.setCategory(category);
        sp.setDescription(description);
        speciesRepo.save(sp);

        PlantLocation loc = locationRepo.findBySpeciesId(id).orElse(null);
        if (loc == null) {
            loc = new PlantLocation();
            loc.setSpecies(sp);
        }
        loc.setName(name);
        loc.setLongitude(longitude);
        loc.setLatitude(latitude);
        Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude));
        loc.setGeom(point);

        if (file != null && !file.isEmpty()) {
            String filename = "plants/" + UUID.randomUUID() + "_" + sanitizeFilename(file.getOriginalFilename());
            String url = storage.upload(filename, file.getInputStream());
            loc.setImage(url);
            PlantImage img = new PlantImage();
            img.setCategory(category);
            img.setImageUrl(url);
            imageRepo.save(img);
        }

        locationRepo.save(loc);
        return detail(id);
    }

    /** 追加图片到类型库 */
    public String uploadImage(String speciesId, MultipartFile file) throws IOException {
        PlantSpecies sp = speciesRepo.findById(speciesId)
                .orElseThrow(() -> new RuntimeException("树木不存在"));
        String filename = "plants/" + UUID.randomUUID() + "_" + sanitizeFilename(file.getOriginalFilename());
        String url = storage.upload(filename, file.getInputStream());

        PlantImage img = new PlantImage();
        img.setCategory(sp.getCategory());
        img.setImageUrl(url);
        imageRepo.save(img);
        return url;
    }

    // ---- 内部方法 ----

    /** 清理文件名中的空格和特殊字符 */
    private String sanitizeFilename(String original) {
        if (original == null) return "file";
        // 保留中文、字母、数字、点、下划线、连字符，其余替换为下划线
        return original.replaceAll("[\\s]+", "_")       // 空格→下划线
                       .replaceAll("[^a-zA-Z0-9.\\-\\u4e00-\\u9fff_]", "");
    }

    private List<PlantVO> toVOList(List<PlantLocation> locations) {
        if (locations.isEmpty()) return List.of();
        List<PlantVO> vos = new ArrayList<>();
        for (PlantLocation loc : locations) {
            vos.add(toVO(loc.getSpecies(), loc));
        }
        return vos;
    }

    private PlantVO toVO(PlantSpecies sp, PlantLocation loc) {
        PlantVO vo = new PlantVO();
        vo.setId(sp.getId());
        vo.setName(sp.getName());
        vo.setCategory(sp.getCategory());
        vo.setDescription(sp.getDescription());

        if (loc != null) {
            vo.setLocationName(loc.getName());
            vo.setLongitude(loc.getLongitude());
            vo.setLatitude(loc.getLatitude());
            vo.setImage(loc.getImage());
        }

        // 备选图片：专属图片 > 同类型随机
        List<PlantImage> typeImages = imageRepo.findByCategory(sp.getCategory());
        if (loc != null && loc.getImage() != null) {
            vo.setFallbackImage(loc.getImage());
        } else if (!typeImages.isEmpty()) {
            vo.setFallbackImage(typeImages.get(random.nextInt(typeImages.size())).getImageUrl());
        }
        vo.setImages(typeImages.stream().map(PlantImage::getImageUrl).collect(Collectors.toList()));

        return vo;
    }
}
