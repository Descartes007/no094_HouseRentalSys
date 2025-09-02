package org.example.springboot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.springboot.entity.House;
import org.example.springboot.entity.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 统计数据传输对象
 */
public class StatisticsDTO {
    
    /**
     * 仪表盘统计数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardStatistics {
        // 用户统计
        private Long userCount;
        private Long tenantCount;
        private Long landlordCount;
        
        // 房源统计
        private Long houseCount;
        private Long availableHouseCount;
        private Long rentedHouseCount;
        
        // 订单统计
        private Long orderCount;
        private Long monthOrderCount;
        private Long pendingOrderCount;
        
        // 交易统计
        private BigDecimal totalAmount;
        private BigDecimal monthAmount;
    }
    
    /**
     * 最近租赁记录
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentLease {
        private Long id;
        private Long houseId;
        private String houseName;
        private String houseImage;
        private Long tenantId;
        private String tenantName;
        private Long landlordId;
        private String landlordName;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private BigDecimal amount;
        private Integer status;
        private LocalDateTime createTime;
    }
    
    /**
     * 月度订单统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyOrderStatistics {
        private List<String> months;
        private List<Long> orderCounts;
    }
    
    /**
     * 房源类型分布
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HouseTypeData {
        private String name;
        private Long count;
    }
} 