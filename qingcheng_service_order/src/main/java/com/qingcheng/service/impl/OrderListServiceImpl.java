package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.qingcheng.dao.OrderItemMapper;
import com.qingcheng.dao.OrderMapper;
import com.qingcheng.pojo.order.Order;
import com.qingcheng.pojo.order.OrderItem;
import com.qingcheng.pojo.order.OrderList;
import com.qingcheng.service.order.OrderListService;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.List;

@Service
public class OrderListServiceImpl implements OrderListService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;

    @Override
    public List<OrderList> getList(String[] ids) {
        List<OrderList> orderListList = new ArrayList<>();
        for (String id : ids) {
            Order order = orderMapper.selectByPrimaryKey(id);
            if (!"null".equals(order) && !"".equals(id)) {
                OrderList orderList = new OrderList();
                Example example = new Example(OrderItem.class);
                Example.Criteria criteria = example.createCriteria();
                criteria.andEqualTo("orderId", id);
                OrderItem orderItem = orderItemMapper.selectOneByExample(example);
                if (orderItem != null) {
                    orderList.setOrderItem(orderItem);
                    orderList.setOrder(order);
                    orderListList.add(orderList);
                } else {
                    throw new RuntimeException("没找到订单详情");
                }
            }
        }
        return orderListList;
    }
}
