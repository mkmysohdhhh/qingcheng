package com.qingcheng.dao;

import com.qingcheng.pojo.order.CategoryReport;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface CategoryReportMapper extends Mapper<CategoryReport> {
    @Select("SELECT category_id1 ,category_id2,category_id3 ,DATE_FORMAT(o.`pay_time`,'%Y-%m-%d' ) count_date,SUM(oi.num) num,SUM(oi.pay_money) money " +
            "FROM tb_order_item  oi, tb_order o " +
            "WHERE  oi.`order_id`=o.`id` AND o.`pay_status`='1' AND DATE_FORMAT(o.`pay_time`,'%Y-%m-%d' ) =#{localDate} " +
            "GROUP  BY `category_id1`,`category_id2`,`category_id3`,DATE_FORMAT(o.`pay_time`,'%Y-%m-%d' ) ")
    List<CategoryReport> categoryReport(@Param("localDate") LocalDate localDate);

    @Select({"SELECT category_id1 categoryId1,v.name name,SUM(num) num ,SUM(money) money FROM `tb_category_report` ,v_category v " +
            "WHERE count_date>=#{date1} and count_date<=#{date2}and category_id1=v.id " +
            "GROUP BY category_id1,v.name"})
    List<Map> category1Count(@Param("date1") String date1, @Param("date2")String date2);
}
