package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.example.springboot.entity.HouseType;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.HouseMapper;
import org.example.springboot.mapper.HouseTypeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 房屋类型服务实现类
 */
@Service
public class HouseTypeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HouseTypeService.class);
    
    @Resource
    private HouseTypeMapper houseTypeMapper;
    
    @Resource
    private HouseMapper houseMapper;
    
    /**
     * 分页查询房屋类型
     * @param name 类型名称
     * @param currentPage 当前页
     * @param size 每页大小
     * @return 分页数据
     */
    public Page<HouseType> getHouseTypesByPage(String name, Integer currentPage, Integer size) {
        LambdaQueryWrapper<HouseType> queryWrapper = new LambdaQueryWrapper<>();
        
        // 添加查询条件
        if (StringUtils.isNotBlank(name)) {
            queryWrapper.like(HouseType::getName, name);
        }
        
        // 按创建时间降序排序
        queryWrapper.orderByDesc(HouseType::getCreateTime);
        
        return houseTypeMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
    }
    
    /**
     * 获取所有房屋类型
     * @return 房屋类型列表
     */
    public List<HouseType> getAllHouseTypes() {
        return houseTypeMapper.selectList(new LambdaQueryWrapper<HouseType>()
                .orderByAsc(HouseType::getId));
    }
    
    /**
     * 根据ID获取房屋类型
     * @param id 类型ID
     * @return 房屋类型
     */
    public HouseType getHouseTypeById(Long id) {
        HouseType houseType = houseTypeMapper.selectById(id);
        if (houseType == null) {
            throw new ServiceException("房屋类型不存在");
        }
        return houseType;
    }
    
    /**
     * 创建房屋类型
     * @param houseType 房屋类型信息
     */
    @Transactional
    public void createHouseType(HouseType houseType) {
        // 检查名称是否已存在
        LambdaQueryWrapper<HouseType> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(HouseType::getName, houseType.getName());
        if (houseTypeMapper.exists(queryWrapper)) {
            throw new ServiceException("房屋类型名称已存在");
        }
        
        // 设置创建和更新时间
        LocalDateTime now = LocalDateTime.now();
        houseType.setCreateTime(now);
        houseType.setUpdateTime(now);
        
        int result = houseTypeMapper.insert(houseType);
        if (result <= 0) {
            throw new ServiceException("创建房屋类型失败");
        }
    }
    
    /**
     * 更新房屋类型
     * @param id 类型ID
     * @param houseType 房屋类型信息
     */
    @Transactional
    public void updateHouseType(Long id, HouseType houseType) {
        // 检查房屋类型是否存在
        HouseType existingType = getHouseTypeById(id);
        
        // 检查名称是否已被其他记录使用
        if (!existingType.getName().equals(houseType.getName())) {
            LambdaQueryWrapper<HouseType> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(HouseType::getName, houseType.getName());
            if (houseTypeMapper.exists(queryWrapper)) {
                throw new ServiceException("房屋类型名称已存在");
            }
        }
        
        // 设置ID和更新时间
        houseType.setId(id);
        houseType.setUpdateTime(LocalDateTime.now());
        houseType.setCreateTime(existingType.getCreateTime()); // 保留原创建时间
        
        int result = houseTypeMapper.updateById(houseType);
        if (result <= 0) {
            throw new ServiceException("更新房屋类型失败");
        }
    }
    
    /**
     * 删除房屋类型
     * @param id 类型ID
     */
    @Transactional
    public void deleteHouseType(Long id) {
        // 检查房屋类型是否存在
        getHouseTypeById(id);
        
        // 检查是否有房屋关联了该类型
        LambdaQueryWrapper<org.example.springboot.entity.House> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(org.example.springboot.entity.House::getTypeId, id);
        if (houseMapper.exists(queryWrapper)) {
            throw new ServiceException("该房屋类型下有关联的房屋，无法删除");
        }
        
        int result = houseTypeMapper.deleteById(id);
        if (result <= 0) {
            throw new ServiceException("删除房屋类型失败");
        }
    }
} 