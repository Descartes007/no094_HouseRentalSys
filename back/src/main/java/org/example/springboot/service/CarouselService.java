package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.springboot.entity.Carousel;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.CarouselMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CarouselService {
    
    @Resource
    private CarouselMapper carouselMapper;
    
    /**
     * 分页查询轮播图
     */
    public Page<Carousel> getCarouselsByPage(String title, Integer status, Integer currentPage, Integer size) {
        LambdaQueryWrapper<Carousel> queryWrapper = new LambdaQueryWrapper<>();
        
        // 添加查询条件
        if (title != null && !title.isEmpty()) {
            queryWrapper.like(Carousel::getTitle, title);
        }
        
        if (status != null) {
            queryWrapper.eq(Carousel::getStatus, status);
        }
        
        // 按创建时间降序排序
        queryWrapper.orderByDesc(Carousel::getCreateTime);
        
        return carouselMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
    }
    
    /**
     * 获取所有启用的轮播图
     */
    public List<Carousel> getActiveCarousels() {
        LambdaQueryWrapper<Carousel> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Carousel::getStatus, 1);
        queryWrapper.orderByAsc(Carousel::getId);
        return carouselMapper.selectList(queryWrapper);
    }
    
    /**
     * 根据ID获取轮播图
     */
    public Carousel getCarouselById(Long id) {
        Carousel carousel = carouselMapper.selectById(id);
        if (carousel == null) {
            throw new ServiceException("轮播图不存在");
        }
        return carousel;
    }
    
    /**
     * 添加轮播图
     */
    public void addCarousel(Carousel carousel) {
        carouselMapper.insert(carousel);
    }
    
    /**
     * 更新轮播图
     */
    public void updateCarousel(Carousel carousel) {
        Carousel existingCarousel = getCarouselById(carousel.getId());
        carouselMapper.updateById(carousel);
    }
    
    /**
     * 删除轮播图
     */
    public void deleteCarousel(Long id) {
        Carousel carousel = getCarouselById(id);
        carouselMapper.deleteById(id);
    }
    
    /**
     * 更新轮播图状态
     */
    public void updateCarouselStatus(Long id, Integer status) {
        Carousel carousel = getCarouselById(id);
        carousel.setStatus(status);
        carouselMapper.updateById(carousel);
    }
} 