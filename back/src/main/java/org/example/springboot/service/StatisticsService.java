package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.dto.StatisticsDTO;
import org.example.springboot.entity.*;
import org.example.springboot.mapper.*;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StatisticsService {
    
    @Resource
    private UserMapper userMapper;
    
    @Resource
    private HouseMapper houseMapper;
    
    @Resource
    private OrderMapper orderMapper;
    
    @Resource
    private TransactionMapper transactionMapper;
    
    @Resource
    private LeaseRecordMapper leaseRecordMapper;
    
    @Resource
    private HouseTypeMapper houseTypeMapper;
    
    /**
     * 获取仪表盘统计数据
     */
    public StatisticsDTO.DashboardStatistics getDashboardStatistics() {
        // 用户统计
        LambdaQueryWrapper<User> userQuery = new LambdaQueryWrapper<>();
        long userCount = userMapper.selectCount(userQuery);
        
        // 租客统计
        LambdaQueryWrapper<User> tenantQuery = new LambdaQueryWrapper<>();
        tenantQuery.eq(User::getRoleCode, "TENANT");
        long tenantCount = userMapper.selectCount(tenantQuery);
        
        // 房东统计
        LambdaQueryWrapper<User> landlordQuery = new LambdaQueryWrapper<>();
        landlordQuery.eq(User::getRoleCode, "LANDLORD");
        long landlordCount = userMapper.selectCount(landlordQuery);
        
        // 房源统计
        LambdaQueryWrapper<House> houseQuery = new LambdaQueryWrapper<>();
        long houseCount = houseMapper.selectCount(houseQuery);
        
        // 可租房源统计
        LambdaQueryWrapper<House> availableHouseQuery = new LambdaQueryWrapper<>();
        availableHouseQuery.eq(House::getStatus, 1); // 状态为1表示可租赁
        long availableHouseCount = houseMapper.selectCount(availableHouseQuery);
        
        // 已租房源统计
        LambdaQueryWrapper<House> rentedHouseQuery = new LambdaQueryWrapper<>();
        rentedHouseQuery.eq(House::getStatus, 2); // 状态为2表示已出租
        long rentedHouseCount = houseMapper.selectCount(rentedHouseQuery);
        
        // 订单统计
        LambdaQueryWrapper<Order> orderQuery = new LambdaQueryWrapper<>();
        long orderCount = orderMapper.selectCount(orderQuery);
        
        // 本月订单统计
        LocalDateTime firstDayOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LambdaQueryWrapper<Order> monthOrderQuery = new LambdaQueryWrapper<>();
        monthOrderQuery.ge(Order::getCreateTime, firstDayOfMonth);
        long monthOrderCount = orderMapper.selectCount(monthOrderQuery);
        
        // 待支付订单统计
        LambdaQueryWrapper<Order> pendingOrderQuery = new LambdaQueryWrapper<>();
        pendingOrderQuery.eq(Order::getStatus, 0); // 状态为0表示待支付
        long pendingOrderCount = orderMapper.selectCount(pendingOrderQuery);
        
        // 交易总额
        LambdaQueryWrapper<Transaction> transactionQuery = new LambdaQueryWrapper<>();
        transactionQuery.eq(Transaction::getType, 1); // 类型为1表示收入
        List<Transaction> transactions = transactionMapper.selectList(transactionQuery);
        BigDecimal totalAmount = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 本月交易额
        LambdaQueryWrapper<Transaction> monthTransactionQuery = new LambdaQueryWrapper<>();
        monthTransactionQuery.eq(Transaction::getType, 1) // 类型为1表示收入
                .ge(Transaction::getCreateTime, firstDayOfMonth);
        List<Transaction> monthTransactions = transactionMapper.selectList(monthTransactionQuery);
        BigDecimal monthAmount = monthTransactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return StatisticsDTO.DashboardStatistics.builder()
                .userCount(userCount)
                .tenantCount(tenantCount)
                .landlordCount(landlordCount)
                .houseCount(houseCount)
                .availableHouseCount(availableHouseCount)
                .rentedHouseCount(rentedHouseCount)
                .orderCount(orderCount)
                .monthOrderCount(monthOrderCount)
                .pendingOrderCount(pendingOrderCount)
                .totalAmount(totalAmount)
                .monthAmount(monthAmount)
                .build();
    }
    
    /**
     * 获取最近租赁记录
     */
    public List<StatisticsDTO.RecentLease> getRecentLeases() {
        LambdaQueryWrapper<LeaseRecord> query = new LambdaQueryWrapper<>();
        query.orderByDesc(LeaseRecord::getCreateTime)
                .last("LIMIT 5"); // 只取最近5条记录
        
        List<LeaseRecord> leases = leaseRecordMapper.selectList(query);
        List<StatisticsDTO.RecentLease> result = new ArrayList<>();
        
        for (LeaseRecord lease : leases) {
            House house = houseMapper.selectById(lease.getHouseId());
            User tenant = userMapper.selectById(lease.getTenantId());
            User landlord = userMapper.selectById(lease.getLandlordId());
            
            if (house != null && tenant != null && landlord != null) {
                StatisticsDTO.RecentLease recentLease = StatisticsDTO.RecentLease.builder()
                        .id(lease.getId())
                        .houseId(lease.getHouseId())
                        .houseName(house.getTitle())
                        .houseImage(house.getImages())
                        .tenantId(lease.getTenantId())
                        .tenantName(tenant.getName())
                        .landlordId(lease.getLandlordId())
                        .landlordName(landlord.getName())
                        .startDate(lease.getStartDate().atStartOfDay())
                        .endDate(lease.getEndDate().atStartOfDay())
                        .amount(lease.getRentAmount())
                        .status(lease.getStatus())
                        .createTime(lease.getCreateTime())
                        .build();
                
                result.add(recentLease);
            }
        }
        
        return result;
    }
    
    /**
     * 获取月度订单统计
     */
    public StatisticsDTO.MonthlyOrderStatistics getMonthlyOrders() {
        List<String> months = new ArrayList<>();
        List<Long> orderCounts = new ArrayList<>();
        
        // 获取最近6个月的月份
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        
        for (int i = 5; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            String monthStr = month.format(formatter);
            months.add(monthStr);
            
            // 查询该月的订单数量
            LocalDateTime startOfMonth = month.withDayOfMonth(1).atStartOfDay();
            LocalDateTime endOfMonth = month.plusMonths(1).withDayOfMonth(1).atStartOfDay();
            
            LambdaQueryWrapper<Order> query = new LambdaQueryWrapper<>();
            query.ge(Order::getCreateTime, startOfMonth)
                    .lt(Order::getCreateTime, endOfMonth);
            
            long count = orderMapper.selectCount(query);
            orderCounts.add(count);
        }
        
        return StatisticsDTO.MonthlyOrderStatistics.builder()
                .months(months)
                .orderCounts(orderCounts)
                .build();
    }
    
    /**
     * 获取房源类型分布
     */
    public List<StatisticsDTO.HouseTypeData> getHouseTypeDistribution() {
        // 获取所有房屋类型
        List<HouseType> houseTypes = houseTypeMapper.selectList(null);
        List<StatisticsDTO.HouseTypeData> result = new ArrayList<>();
        
        for (HouseType type : houseTypes) {
            // 查询该类型的房源数量
            LambdaQueryWrapper<House> query = new LambdaQueryWrapper<>();
            query.eq(House::getTypeId, type.getId());
            long count = houseMapper.selectCount(query);
            
            StatisticsDTO.HouseTypeData typeData = StatisticsDTO.HouseTypeData.builder()
                    .name(type.getName())
                    .count(count)
                    .build();
            
            result.add(typeData);
        }
        
        return result;
    }
} 