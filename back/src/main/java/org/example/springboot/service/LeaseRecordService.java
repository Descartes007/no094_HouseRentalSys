package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.entity.*;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.HouseMapper;
import org.example.springboot.mapper.LeaseRecordMapper;
import org.example.springboot.mapper.OrderMapper;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.util.ContractGenerator;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class LeaseRecordService {

    @Resource
    private LeaseRecordMapper leaseRecordMapper;

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private HouseMapper houseMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private ContractGenerator contractGenerator;

    @Value("${contract.default.duration:12}")
    private int defaultContractDuration; // 默认合同期限（月）

    /**
     * 根据订单ID查询租赁记录
     */
    public LeaseRecord getByOrderId(Long orderId) {
        LambdaQueryWrapper<LeaseRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LeaseRecord::getOrderId, orderId);
        LeaseRecord leaseRecord = leaseRecordMapper.selectOne(queryWrapper);
        
        if (leaseRecord != null) {
            // 加载关联信息
            loadAssociatedInfo(leaseRecord);
        }
        
        return leaseRecord;
    }

    /**
     * 根据ID查询租赁记录
     */
    public LeaseRecord getById(Long id) {
        LeaseRecord leaseRecord = leaseRecordMapper.selectById(id);
        if (leaseRecord != null) {
            // 加载关联信息
            loadAssociatedInfo(leaseRecord);
        }
        return leaseRecord;
    }

    /**
     * 当订单确认后，创建租赁记录并生成合同
     */
    @Transactional
    public LeaseRecord createLeaseRecord(Order order) {
        // 查询相关信息
        House house = houseMapper.selectById(order.getHouseId());
        User tenant = userMapper.selectById(order.getTenantId());
        User landlord = userMapper.selectById(order.getLandlordId());
        
        if (house == null || tenant == null || landlord == null) {
            throw new ServiceException("创建租赁记录失败：缺少必要信息");
        }
        
        // 创建租赁记录
        LeaseRecord leaseRecord = new LeaseRecord();
        leaseRecord.setOrderId(order.getId());
        leaseRecord.setHouseId(house.getId());
        leaseRecord.setTenantId(tenant.getId());
        leaseRecord.setLandlordId(landlord.getId());
        
        // 设置租期
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusMonths(defaultContractDuration);
        leaseRecord.setStartDate(startDate);
        leaseRecord.setEndDate(endDate);
        
        // 设置租金和支付周期
        leaseRecord.setRentAmount(house.getPrice());
        leaseRecord.setPaymentCycle("MONTHLY"); // 默认月付
        
        // 设置初始状态为租赁中
        leaseRecord.setStatus(1);
        
        // 生成合同
        String contractUrl = contractGenerator.generateContract(leaseRecord, order, house, tenant, landlord);
        leaseRecord.setContractUrl(contractUrl);
        
        // 保存租赁记录
        leaseRecordMapper.insert(leaseRecord);
        
        return leaseRecord;
    }

    /**
     * 分页查询租赁记录（管理员）
     */
    public Page<LeaseRecord> getLeaseRecordsByPage(Integer currentPage, Integer size, Integer status) {
        LambdaQueryWrapper<LeaseRecord> queryWrapper = new LambdaQueryWrapper<>();
        
        if (status != null) {
            queryWrapper.eq(LeaseRecord::getStatus, status);
        }
        
        Page<LeaseRecord> page = leaseRecordMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        
        // 加载关联信息
        for (LeaseRecord record : page.getRecords()) {
            loadAssociatedInfo(record);
        }
        
        return page;
    }

    /**
     * 分页查询租赁记录（房东）
     */
    public Page<LeaseRecord> getLandlordLeaseRecordsByPage(Integer currentPage, Integer size, Integer status) {
        Long userId = JwtTokenUtils.getCurrentUser().getId();
        if (userId == null) {
            throw new ServiceException("获取当前用户信息失败");
        }
        
        LambdaQueryWrapper<LeaseRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LeaseRecord::getLandlordId, userId);
        
        if (status != null) {
            queryWrapper.eq(LeaseRecord::getStatus, status);
        }
        
        Page<LeaseRecord> page = leaseRecordMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        
        // 加载关联信息
        for (LeaseRecord record : page.getRecords()) {
            loadAssociatedInfo(record);
        }
        
        return page;
    }

    /**
     * 分页查询租赁记录（租客）
     */
    public Page<LeaseRecord> getTenantLeaseRecordsByPage(Integer currentPage, Integer size, Integer status) {
        Long userId = JwtTokenUtils.getCurrentUser().getId();
        if (userId == null) {
            throw new ServiceException("获取当前用户信息失败");
        }
        
        LambdaQueryWrapper<LeaseRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LeaseRecord::getTenantId, userId);
        
        if (status != null) {
            queryWrapper.eq(LeaseRecord::getStatus, status);
        }
        
        Page<LeaseRecord> page = leaseRecordMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        
        // 加载关联信息
        for (LeaseRecord record : page.getRecords()) {
            loadAssociatedInfo(record);
        }
        
        return page;
    }

    /**
     * 提交租赁评价
     */
    @Transactional
    public LeaseRecord submitEvaluation(Long id, Integer score, String content) {
        Long userId = JwtTokenUtils.getCurrentUser().getId();
        if (userId == null) {
            throw new ServiceException("获取当前用户信息失败");
        }
        
        LeaseRecord leaseRecord = leaseRecordMapper.selectById(id);
        if (leaseRecord == null) {
            throw new ServiceException("租赁记录不存在");
        }
        
        // 验证是否是该租赁记录的租客
        if (!leaseRecord.getTenantId().equals(userId)) {
            throw new ServiceException("您无权评价此租赁记录");
        }
        
        // 更新评价信息
        leaseRecord.setEvaluationScore(score);
        leaseRecord.setEvaluationContent(content);
        leaseRecordMapper.updateById(leaseRecord);
        
        return leaseRecord;
    }

    /**
     * 加载租赁记录关联信息
     */
    private void loadAssociatedInfo(LeaseRecord leaseRecord) {
        // 加载房屋信息
        House house = houseMapper.selectById(leaseRecord.getHouseId());
        leaseRecord.setHouse(house);
        
        // 加载租客信息
        User tenant = userMapper.selectById(leaseRecord.getTenantId());
        leaseRecord.setTenant(tenant);
        
        // 加载房东信息
        User landlord = userMapper.selectById(leaseRecord.getLandlordId());
        leaseRecord.setLandlord(landlord);
        
        // 加载订单信息
        Order order = orderMapper.selectById(leaseRecord.getOrderId());
        leaseRecord.setOrder(order);
    }

    /**
     * 为租赁记录手动生成合同
     */
    @Transactional
    public LeaseRecord generateContractForLeaseRecord(Long id) {
        LeaseRecord leaseRecord = leaseRecordMapper.selectById(id);
        if (leaseRecord == null) {
            throw new ServiceException("租赁记录不存在");
        }
        
        // 查询相关信息
        House house = houseMapper.selectById(leaseRecord.getHouseId());
        User tenant = userMapper.selectById(leaseRecord.getTenantId());
        User landlord = userMapper.selectById(leaseRecord.getLandlordId());
        Order order = orderMapper.selectById(leaseRecord.getOrderId());
        
        if (house == null || tenant == null || landlord == null || order == null) {
            throw new ServiceException("生成合同失败：缺少必要信息");
        }
        
        // 生成合同
        String contractUrl = contractGenerator.generateContract(leaseRecord, order, house, tenant, landlord);
        if (contractUrl == null) {
            throw new ServiceException("合同生成失败，请稍后重试");
        }
        
        // 更新租赁记录的合同URL
        leaseRecord.setContractUrl(contractUrl);
        leaseRecordMapper.updateById(leaseRecord);
        
        log.info("手动生成合同成功，租赁记录ID：{}，合同URL：{}", id, contractUrl);
        
        return leaseRecord;
    }

    /**
     * 生成租赁合同并返回URL
     */
    public String generateContract(Long id) {
        LeaseRecord leaseRecord = generateContractForLeaseRecord(id);
        return leaseRecord != null ? leaseRecord.getContractUrl() : null;
    }
} 