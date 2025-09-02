package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.example.springboot.entity.House;
import org.example.springboot.entity.HouseType;
import org.example.springboot.entity.User;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.HouseMapper;
import org.example.springboot.mapper.HouseTypeMapper;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.util.JwtTokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 房屋服务实现类
 */
@Service
public class HouseService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HouseService.class);
    
    @Resource
    private HouseMapper houseMapper;
    
    @Resource
    private HouseTypeMapper houseTypeMapper;
    
    @Resource
    private UserMapper userMapper;
    
    /**
     * 分页查询房屋信息
     * @param title 房屋标题
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @param typeId 房屋类型ID
     * @param status 房屋状态
     * @param currentPage 当前页
     * @param size 每页大小
     * @return 分页数据
     */
    public Page<House> getHousesByPage(String title, Long landLordId, BigDecimal minPrice, BigDecimal maxPrice,
                                       Long typeId, Integer status, Integer currentPage, Integer size) {
        LambdaQueryWrapper<House> queryWrapper = new LambdaQueryWrapper<>();
        
        // 添加查询条件
        if (StringUtils.isNotBlank(title)) {
            queryWrapper.like(House::getTitle, title);
        }
        
        if (minPrice != null) {
            queryWrapper.ge(House::getPrice, minPrice);
        }
        
        if (maxPrice != null) {
            queryWrapper.le(House::getPrice, maxPrice);
        }
        
        // 只有typeId非null时才添加条件
        if (typeId != null) {
            queryWrapper.eq(House::getTypeId, typeId);
        }
        
        if (status != null) {
            queryWrapper.eq(House::getStatus, status);
        }
        if(landLordId != null) {
            queryWrapper.eq(House::getLandlordId, landLordId);
        }

        
        // 按创建时间降序排序
        queryWrapper.orderByDesc(House::getCreateTime);
        
        Page<House> page = houseMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        
        // 填充房屋类型名称和房东姓名
        page.getRecords().forEach(this::fillHouseInfo);
        
        return page;
    }
    
    /**
     * 房东查询自己的房屋
     * @param title 房屋标题
     * @param status 房屋状态
     * @param currentPage 当前页
     * @param size 每页大小
     * @return 分页数据
     */
    public Page<House> getLandlordHouses(String title, Integer status, Integer currentPage, Integer size) {
        // 获取当前登录用户
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("获取当前用户信息失败");
        }
        
        LambdaQueryWrapper<House> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(House::getLandlordId, currentUser.getId());
        
        if (StringUtils.isNotBlank(title)) {
            queryWrapper.like(House::getTitle, title);
        }
        
        if (status != null) {
            queryWrapper.eq(House::getStatus, status);
        }
        
        queryWrapper.orderByDesc(House::getCreateTime);
        
        Page<House> page = houseMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        
        // 填充房屋类型名称和房东姓名
        page.getRecords().forEach(this::fillHouseInfo);
        
        return page;
    }
    
    /**
     * 根据ID获取房屋详情
     * @param id 房屋ID
     * @return 房屋信息
     */
    public House getHouseById(Long id) {
        House house = houseMapper.selectById(id);
        if (house == null) {
            throw new ServiceException("房屋不存在");
        }
        
        // 填充房屋类型名称和房东姓名
        fillHouseInfo(house);
        
        return house;
    }
    
    /**
     * 创建房屋
     * @param house 房屋信息
     */
    @Transactional
    public void createHouse(House house) {
        // 获取当前登录用户作为房东
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("获取当前用户信息失败");
        }
        
        // 检查房屋类型是否存在
        HouseType houseType = houseTypeMapper.selectById(house.getTypeId());
        if (houseType == null) {
            throw new ServiceException("所选房屋类型不存在");
        }
        
        // 设置房东ID和初始状态
        house.setLandlordId(currentUser.getId());
        house.setStatus(house.getStatus() != null ? house.getStatus() : 1); // 默认为待出租状态
        
        // 设置创建和更新时间
        LocalDateTime now = LocalDateTime.now();
        house.setCreateTime(now);
        house.setUpdateTime(now);
        
        int result = houseMapper.insert(house);
        if (result <= 0) {
            throw new ServiceException("创建房屋失败");
        }
    }
    
    /**
     * 更新房屋信息
     * @param id 房屋ID
     * @param house 房屋信息
     */
    @Transactional
    public void updateHouse(Long id, House house) {
        // 获取当前登录用户
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("获取当前用户信息失败");
        }
        
        // 检查房屋是否存在
        House existingHouse = houseMapper.selectById(id);
        if (existingHouse == null) {
            throw new ServiceException("房屋不存在");
        }
        
        // 校验房屋类型是否存在
        if (house.getTypeId() != null && !house.getTypeId().equals(existingHouse.getTypeId())) {
            HouseType houseType = houseTypeMapper.selectById(house.getTypeId());
            if (houseType == null) {
                throw new ServiceException("所选房屋类型不存在");
            }
        }
        
        // 检查权限：只有管理员或房东可以修改
        if (!"ADMIN".equals(currentUser.getRoleCode()) && !currentUser.getId().equals(existingHouse.getLandlordId())) {
            throw new ServiceException("无权修改该房屋信息");
        }
        
        // 设置ID和更新时间，保留创建时间和房东ID
        house.setId(id);
        house.setUpdateTime(LocalDateTime.now());
        house.setCreateTime(existingHouse.getCreateTime());
        house.setLandlordId(existingHouse.getLandlordId());
        
        int result = houseMapper.updateById(house);
        if (result <= 0) {
            throw new ServiceException("更新房屋信息失败");
        }
    }
    
    /**
     * 修改房屋状态
     * @param id 房屋ID
     * @param status 状态(0:下架,1:待出租,2:已出租)
     */
    @Transactional
    public void updateHouseStatus(Long id, Integer status) {
        // 获取当前登录用户
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("获取当前用户信息失败");
        }
        
        // 检查房屋是否存在
        House existingHouse = houseMapper.selectById(id);
        if (existingHouse == null) {
            throw new ServiceException("房屋不存在");
        }
        
        // 检查权限：只有管理员或房东可以修改
        if (!"ADMIN".equals(currentUser.getRoleCode()) && !currentUser.getId().equals(existingHouse.getLandlordId())) {
            throw new ServiceException("无权修改该房屋状态");
        }
        
        // 检查状态值是否有效
        if (status < 0 || status > 2) {
            throw new ServiceException("无效的房屋状态值");
        }
        
        House house = new House();
        house.setId(id);
        house.setStatus(status);
        house.setUpdateTime(LocalDateTime.now());
        
        int result = houseMapper.updateById(house);
        if (result <= 0) {
            throw new ServiceException("更新房屋状态失败");
        }
    }
    
    /**
     * 删除房屋
     * @param id 房屋ID
     */
    @Transactional
    public void deleteHouse(Long id) {
        // 获取当前登录用户
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("获取当前用户信息失败");
        }
        
        // 检查房屋是否存在
        House existingHouse = houseMapper.selectById(id);
        if (existingHouse == null) {
            throw new ServiceException("房屋不存在");
        }
        
        // 检查权限：只有管理员或房东可以删除
        if (!"ADMIN".equals(currentUser.getRoleCode()) && !currentUser.getId().equals(existingHouse.getLandlordId())) {
            throw new ServiceException("无权删除该房屋");
        }
        
        // 已出租状态的房屋不能删除
        if (existingHouse.getStatus() == 2) {
            throw new ServiceException("已出租的房屋不能删除");
        }
        
        int result = houseMapper.deleteById(id);
        if (result <= 0) {
            throw new ServiceException("删除房屋失败");
        }
    }
    
    /**
     * 填充房屋类型名称和房东姓名
     * @param house 房屋信息
     */
    private void fillHouseInfo(House house) {
        if (house == null) {
            return;
        }
        
        // 填充房屋类型名称
        if (house.getTypeId() != null) {
            HouseType houseType = houseTypeMapper.selectById(house.getTypeId());
            if (houseType != null) {
                house.setTypeName(houseType.getName());
            }
        }
        
        // 填充房东姓名
        if (house.getLandlordId() != null) {
            User landlord = userMapper.selectById(house.getLandlordId());
            if (landlord != null) {
                house.setLandlordName(landlord.getName());
                house.setLandlordImg(landlord.getAvatar());
            }
        }
    }
} 