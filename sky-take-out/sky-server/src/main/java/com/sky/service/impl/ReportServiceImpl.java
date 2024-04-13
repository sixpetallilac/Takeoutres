package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import io.swagger.models.auth.In;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;

    /**
     * amount统计
     * @param begin
     * @param end
     * @return
     */
    public TurnoverReportVO turnOver(LocalDate begin, LocalDate end) {
        List<Double> turnoverList = new ArrayList<>();
        List<LocalDate> times = new ArrayList<>();
        times.add(begin);
        //时间放入list 查询时候再fore循环查询mapper
        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            times.add(begin);
        }

        for (LocalDate date : times){
            // select sum(amount) from orders where ordertime > ? and ordertime < ? and status = 5
            // 《严谨》《时间点 including》
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date,LocalTime.MAX);
            //map is used for mapper
            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end",endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }

        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(times,","))
                .turnoverList(StringUtils.join(turnoverList,","))
                .build();
    }

    /**
     * 用户统计
     * @param begin
     * @param end
     * @return
     */
    public UserReportVO userReport(LocalDate begin, LocalDate end) {
        //from start till end dateList
        List<LocalDate> times = new ArrayList<>();
        times.add(begin);
        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            times.add(begin);
        }
        //每天的用户数量 select count(id）from user where
        List<Integer> totalUserList = new ArrayList<>();
        //新增用户数量
        List<Integer> newUserList = new ArrayList<>();

        //iterator loop by date
        for (LocalDate ld : times){
            LocalDateTime start = LocalDateTime.of(ld, LocalTime.MIN);
            LocalDateTime ending = LocalDateTime.of(ld, LocalTime.MAX);
            //put end first and you'll get total user number
            Map map = new HashMap();
            map.put("end",ending);
            Integer total = userMapper.countByMap(map);

            map.put("begin",start);
            Integer newUser = userMapper.countByMap(map);
            totalUserList.add(total);
            newUserList.add(newUser);
        }
        return UserReportVO.builder()
                .dateList(StringUtils.join(times,","))
                .totalUserList(StringUtils.join(totalUserList,","))
                .newUserList(StringUtils.join(newUserList,","))
                .build();
    }

    /**
     * 订单统计
     * @param begin
     * @param end
     * @return
     */
    public OrderReportVO orderReport(LocalDate begin, LocalDate end) {
        //daily order count total
        List<Integer> orderCountList = new ArrayList<>();
        //validOrderCountList
        List<Integer> validOrderCountList = new ArrayList<>();

        List<LocalDate> times = new ArrayList<>();
        times.add(begin);
        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            times.add(begin);
        }

        for (LocalDate ld : times){
            LocalDateTime start = LocalDateTime.of(ld, LocalTime.MIN);
            LocalDateTime ending = LocalDateTime.of(ld, LocalTime.MAX);
            //查询区间订单总数
            Integer orderCounts = getOrderCount(start, ending, null);
            //查询区间有效订单数
            Integer validOrderCount = getOrderCount(start, ending, Orders.COMPLETED);

            //塞入list 作为vo return
            orderCountList.add(orderCounts);
            validOrderCountList.add(validOrderCount);
        }
        //统计 counting
        //get interval totalCounts
        Integer totalCounts = orderCountList.stream().reduce(Integer::sum).get();
        //get interval validOrderCounts
        Integer validOrderCounts = validOrderCountList.stream().reduce(Integer::sum).get();
        //orderCompleteRate
        Double orderCompleteRate = 0.0;
        if (totalCounts != 0){
            orderCompleteRate = validOrderCounts.doubleValue()/totalCounts;
        }
        return OrderReportVO.builder()
                .dateList(StringUtils.join(times,","))
                .orderCountList(StringUtils.join(orderCountList,","))
                .validOrderCountList(StringUtils.join(validOrderCountList,","))
                .totalOrderCount(totalCounts)
                .validOrderCount(validOrderCounts)
                .orderCompletionRate(orderCompleteRate)
                .build();
    }

    private Integer getOrderCount(LocalDateTime begin, LocalDateTime end, Integer status){
        Map map = new HashMap<>();
        map.put("begin", begin);
        map.put("end", end);
        map.put("status", status);
        Integer result = orderMapper.countByMap(map);
        return result;
    }

    /**
     * 销量前10统计
     * @param begin
     * @param end
     * @return
     */
    public SalesTop10ReportVO salesTop10Report(LocalDate begin, LocalDate end) {
        LocalDateTime start = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime ending = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> goodsSalesDTOList = orderMapper.salesTop10Report(start, ending);
        List<String> names = goodsSalesDTOList.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> number = goodsSalesDTOList.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());

        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(names,","))
                .numberList(StringUtils.join(number,","))
                .build();
    }

}
