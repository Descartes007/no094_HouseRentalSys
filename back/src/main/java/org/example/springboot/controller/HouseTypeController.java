package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.HouseType;
import org.example.springboot.entity.User;
import org.example.springboot.service.HouseTypeService;
import org.example.springboot.util.JwtTokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name="房屋类型管理接口")
@RestController
@RequestMapping("/house-types")
public class HouseTypeController {
    private static final Logger LOGGER = LoggerFactory.getLogger(HouseTypeController.class);
    
    @Resource
    private HouseTypeService houseTypeService;
    
    @Operation(summary = "分页查询房屋类型")
    @GetMapping("/page")
    public Result<?> getHouseTypesByPage(
            @RequestParam(defaultValue = "") String name,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<HouseType> page = houseTypeService.getHouseTypesByPage(name, currentPage, size);
        return Result.success(page);
    }
    
    @Operation(summary = "获取所有房屋类型")
    @GetMapping
    public Result<?> getAllHouseTypes() {
        List<HouseType> list = houseTypeService.getAllHouseTypes();
        return Result.success(list);
    }
    
    @Operation(summary = "根据ID获取房屋类型")
    @GetMapping("/{id}")
    public Result<?> getHouseTypeById(@PathVariable Long id) {
        HouseType houseType = houseTypeService.getHouseTypeById(id);
        return Result.success(houseType);
    }
    
    @Operation(summary = "创建房屋类型")
    @PostMapping
    public Result<?> createHouseType(@RequestBody HouseType houseType) {
        // 检查权限，只有管理员可以创建房屋类型
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null || !"ADMIN".equals(currentUser.getRoleCode())) {
            return Result.error("无权创建房屋类型");
        }
        
        houseTypeService.createHouseType(houseType);
        return Result.success("创建成功");
    }
    
    @Operation(summary = "更新房屋类型")
    @PutMapping("/{id}")
    public Result<?> updateHouseType(@PathVariable Long id, @RequestBody HouseType houseType) {
        // 检查权限，只有管理员可以更新房屋类型
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null || !"ADMIN".equals(currentUser.getRoleCode())) {
            return Result.error("无权更新房屋类型");
        }
        
        houseTypeService.updateHouseType(id, houseType);
        return Result.success("更新成功");
    }
    
    @Operation(summary = "删除房屋类型")
    @DeleteMapping("/{id}")
    public Result<?> deleteHouseType(@PathVariable Long id) {
        // 检查权限，只有管理员可以删除房屋类型
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null || !"ADMIN".equals(currentUser.getRoleCode())) {
            return Result.error("无权删除房屋类型");
        }
        
        houseTypeService.deleteHouseType(id);
        return Result.success("删除成功");
    }
} 