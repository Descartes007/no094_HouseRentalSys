package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.Carousel;
import org.example.springboot.service.CarouselService;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "轮播图管理接口")
@RestController
public class CarouselController {
    
    @Resource
    private CarouselService carouselService;
    
    @Operation(summary = "分页查询轮播图")
    @GetMapping("/admin/carousels")
    public Result<?> getCarouselsByPage(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<Carousel> page = carouselService.getCarouselsByPage(title, status, currentPage, size);
        return Result.success(page);
    }
    
    @Operation(summary = "获取所有启用的轮播图")
    @GetMapping("/carousels")
    public Result<?> getActiveCarousels() {
        List<Carousel> carousels = carouselService.getActiveCarousels();
        return Result.success(carousels);
    }
    
    @Operation(summary = "根据ID获取轮播图")
    @GetMapping("/admin/carousels/{id}")
    public Result<?> getCarouselById(@PathVariable Long id) {
        Carousel carousel = carouselService.getCarouselById(id);
        return Result.success(carousel);
    }
    
    @Operation(summary = "添加轮播图")
    @PostMapping("/admin/carousels")
    public Result<?> addCarousel(@RequestBody Carousel carousel) {
        carouselService.addCarousel(carousel);
        return Result.success();
    }
    
    @Operation(summary = "更新轮播图")
    @PutMapping("/admin/carousels/{id}")
    public Result<?> updateCarousel(@PathVariable Long id, @RequestBody Carousel carousel) {
        carousel.setId(id);
        carouselService.updateCarousel(carousel);
        return Result.success();
    }
    
    @Operation(summary = "删除轮播图")
    @DeleteMapping("/admin/carousels/{id}")
    public Result<?> deleteCarousel(@PathVariable Long id) {
        carouselService.deleteCarousel(id);
        return Result.success();
    }
    
    @Operation(summary = "更新轮播图状态")
    @PutMapping("/admin/carousels/{id}/status")
    public Result<?> updateCarouselStatus(@PathVariable Long id, @RequestParam Integer status) {
        carouselService.updateCarouselStatus(id, status);
        return Result.success();
    }
} 