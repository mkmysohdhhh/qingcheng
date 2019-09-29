package com.qingcheng.service.order;

import com.qingcheng.pojo.order.OrderList;

import java.util.List;

/**
 * 展示订单页和订单详情页
 */
public interface OrderListService {
    List<OrderList> getList(String[] ids);
}
