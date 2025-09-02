package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.common.Result;
import org.example.springboot.dto.StatisticsDTO;
import org.example.springboot.service.StatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/statistics")
@Tag(name = "统计数据接口", description = "提供系统各类统计数据")
public class StatisticsController {
    
    @Resource
    private StatisticsService statisticsService;
    
    @GetMapping("/dashboard")
    @Operation(summary = "获取仪表盘统计数据", description = "获取用户、房源、订单、交易等统计数据")
    public Result<StatisticsDTO.DashboardStatistics> getDashboardStatistics() {
        log.info("获取仪表盘统计数据");
        StatisticsDTO.DashboardStatistics statistics = statisticsService.getDashboardStatistics();
        return Result.success(statistics);
    }
    
    @GetMapping("/recent-leases")
    @Operation(summary = "获取最近租赁记录", description = "获取最近5条租赁记录")
    public Result<List<StatisticsDTO.RecentLease>> getRecentLeases() {
        log.info("获取最近租赁记录");
        List<StatisticsDTO.RecentLease> leases = statisticsService.getRecentLeases();
        return Result.success(leases);
    }
    
    @GetMapping("/monthly-orders")
    @Operation(summary = "获取月度订单统计", description = "获取最近6个月的订单统计数据")
    public Result<StatisticsDTO.MonthlyOrderStatistics> getMonthlyOrders() {
        log.info("获取月度订单统计");
        StatisticsDTO.MonthlyOrderStatistics statistics = statisticsService.getMonthlyOrders();
        return Result.success(statistics);
    }
    
    @GetMapping("/house-types")
    @Operation(summary = "获取房源类型分布", description = "获取各类型房源的数量分布")
    public Result<List<StatisticsDTO.HouseTypeData>> getHouseTypeDistribution() {
        log.info("获取房源类型分布");
        List<StatisticsDTO.HouseTypeData> distribution = statisticsService.getHouseTypeDistribution();
        return Result.success(distribution);
    }
} 