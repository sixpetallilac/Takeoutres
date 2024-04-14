package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import io.swagger.models.auth.In;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
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
    @Autowired
    private WorkspaceService workspaceService;
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
        //时间不需要作为返回数据 无loop
        LocalDateTime start = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime ending = LocalDateTime.of(end, LocalTime.MAX);

        //结果统一返回list, 拆分处理list方便后续StringUtils.join
        List<GoodsSalesDTO> goodsSalesDTOList = orderMapper.salesTop10Report(start, ending);
        List<String> names = goodsSalesDTOList.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> number = goodsSalesDTOList.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());

        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(names,","))
                .numberList(StringUtils.join(number,","))
                .build();
    }

    /**
     * 倒出运营数据报表
     * @param response
     */
    public void exportBusinessData(HttpServletResponse response) {
        //查询->写入excel->输出流下载
        //查询 30days
        LocalDate begin = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now().minusDays(1);
        BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(begin, LocalTime.MIN), LocalDateTime.of(end, LocalTime.MAX));

        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        //写入
        try {
            //基于模板创建excel
            XSSFWorkbook excel = new XSSFWorkbook(resourceAsStream);
            //excel 标签页名字
            XSSFSheet sheet = excel.getSheet("sheet1");
            //下标从0开始 第二行 第二个单元格（时间数据填充）
            sheet.getRow(1).getCell(1).setCellValue("时间：" + begin + "至" + end);
            //获得第四行
            XSSFRow row = sheet.getRow(3);
            //填充数据
            row.getCell(2).setCellValue(businessData.getTurnover());
            row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessData.getNewUsers());
            //第五行
            XSSFRow row5 = sheet.getRow(4);
            //填充数据
            row5.getCell(2).setCellValue(businessData.getValidOrderCount());
            row5.getCell(4).setCellValue(businessData.getUnitPrice());

            //每日数据填充
            for (int i = 0; i < 30; i++){
                LocalDate date = begin.plusDays(i);
                BusinessDataVO datas = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
                //获得行数,填充数据
                XSSFRow loopRow = sheet.getRow(7 + i);
                loopRow.getCell(1).setCellValue(date.toString());
                loopRow.getCell(2).setCellValue(datas.getTurnover());
                loopRow.getCell(3).setCellValue(datas.getValidOrderCount());
                loopRow.getCell(4).setCellValue(datas.getOrderCompletionRate());
                loopRow.getCell(5).setCellValue(datas.getUnitPrice());
                loopRow.getCell(6).setCellValue(datas.getNewUsers());

            }


            //下载
            ServletOutputStream outputStream = response.getOutputStream();
            excel.write(outputStream);
            //关闭资源
            outputStream.close();
            excel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }




    }
}
