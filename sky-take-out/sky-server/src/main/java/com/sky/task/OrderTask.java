package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class OrderTask {
    @Autowired
    OrderMapper orderMapper;

    /**
     * 超时order 处理
     */
    @Scheduled(cron = "0 * * * * ?")
    public void processTimeOuter(){
        log.info("定时处理订单：{}", LocalDateTime.now());
        List<Orders> list = orderMapper.getByStatusAndOrderTime(Orders.PENDING_PAYMENT, LocalDateTime.now().plusMinutes(-15));
        if (CollectionUtils.isEmpty(list)){
            for (Orders orders:list){
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("ordertask 超时");
                orders.setOrderTime(LocalDateTime.now());
                orderMapper.update(orders);
            }
        }
    }

    /**
     * 处理自动处于派送中的订单
     *
     */
    @Scheduled(cron = "0 0 1 * * ? ")//每日凌晨1点触发
//    @Scheduled(cron = "1/5 * * * * ? ")//临时测试用
    public void processDeliveryOrder(){
        log.info("定时处理处于派送中的订单：{}",LocalDateTime.now());
        LocalDateTime time = LocalDateTime.now().plusHours(-1);
        List<Orders> list = orderMapper.getByStatusAndOrderTime(Orders.DELIVERY_IN_PROGRESS, time);
        if (!CollectionUtils.isEmpty(list)){
            for (Orders orders : list){
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("ordertask 超时");
                orders.setOrderTime(LocalDateTime.now());
                orderMapper.update(orders);
            }
        }
    }
}
