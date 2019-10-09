package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.qingcheng.dao.OrderItemMapper;
import com.qingcheng.dao.OrderMapper;
import com.qingcheng.pojo.order.Order;
import com.qingcheng.pojo.order.OrderItem;
import com.qingcheng.service.order.OrderMergeAndSplitService;
import com.qingcheng.util.IdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service(interfaceClass = OrderMergeAndSplitService.class)
public class OrderMergeAndSplitServiceImpl implements OrderMergeAndSplitService {
    @Autowired
    private IdWorker idWorker;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;

    /**
     * 订单合并
     *
     * @param OrderId1
     * @param OrderId2
     */
    @Transactional
    @Override
    public void OrderMerge(Integer OrderId1, Integer OrderId2) {
        //数量合计、金额合计、优惠金额、邮费、实付金额、订单更新时间、从订单删除状态
        Order order1 = orderMapper.selectByPrimaryKey(OrderId1);
        Order order2 = orderMapper.selectByPrimaryKey(OrderId2);
        if (order1 != null && order2 != null) {
            Order orderMerge = new Order();
            orderMerge.setTotalNum(order1.getTotalNum() + order2.getTotalNum());
            orderMerge.setTotalMoney(order1.getTotalMoney() + order2.getTotalNum());
            orderMerge.setPreMoney(order1.getPreMoney() + order2.getPreMoney());
            orderMerge.setPostFee(order1.getPostFee() + order2.getPostFee());
            orderMerge.setPayMoney(order1.getPayMoney() + order2.getPayMoney());
            orderMerge.setUpdateTime(new Date());
            orderMapper.updateByPrimaryKeySelective(orderMerge);
            OrderItem orderItem = orderItemMapper.selectByPrimaryKey(order2);
            orderItem.setOrderId(order1.getId());
            orderItemMapper.updateByPrimaryKeySelective(orderItem);
            order2.setOrderStatus("1");
            orderMapper.updateByPrimaryKeySelective(order2);
        } else {
            throw new RuntimeException("检查ID");
        }
    }

    /**
     * 订单拆分
     *
     * @param orderItemList
     */
    @Override
    public void OrderSplit(List<OrderItem> orderItemList) {
        for (OrderItem orderItem : orderItemList) {
            //得到接收的参数[{"id":xxx,"num":xxx},{...}...]
            String id = orderItem.getId();
            Integer num = orderItem.getNum();

            //根据数量和id拆分订单详情，并且需要新拆出一个主订单，新订单的ID需要生成，订单详情的OrderID需要注意
            OrderItem orderItemOld = orderItemMapper.selectByPrimaryKey(id);  //旧的订单详情
            //根据旧订单查询出旧主订单
            Order orderOlder = orderMapper.selectByPrimaryKey(orderItemOld.getOrderId());
            orderOlder.setTotalNum(orderItemOld.getNum()-num);
            orderOlder.setPayMoney(orderItemOld.getNum()*orderItemOld.getPrice());
            //旧主订单详情
            orderItemOld.setNum(orderItemOld.getNum()-num);
            orderItemOld.setMoney(orderItemOld.getNum()*orderItemOld.getPrice());

            //新拆分出的订单详情
            OrderItem orderItemNew = new OrderItem();
            orderItemNew.setId(idWorker.nextId()+"");
            orderItemNew.setNum(num);
            orderItemNew.setPayMoney(num*orderItemOld.getPrice());
            //新拆分出的主订单
            Order orderNew = new Order();
            orderNew.setId(idWorker.nextId()+"");
            orderItemNew.setOrderId(orderNew.getId());
            orderNew.setTotalNum(num);
            orderNew.setPayMoney(num*orderItemOld.getPrice());

            //数据层操作
            orderMapper.updateByPrimaryKeySelective(orderOlder);
            orderItemMapper.updateByPrimaryKeySelective(orderItemOld);
            orderMapper.insertSelective(orderNew);
            orderItemMapper.insertSelective(orderItemNew);
        }
    }
}
