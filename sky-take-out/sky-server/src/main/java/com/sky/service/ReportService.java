package com.sky.service;

import com.sky.vo.OrderReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

import java.time.LocalDate;

public interface ReportService {

    TurnoverReportVO turnOver(LocalDate begin, LocalDate end);

    UserReportVO userReport(LocalDate begin, LocalDate end);

    OrderReportVO orderReport(LocalDate begin, LocalDate end);
}
