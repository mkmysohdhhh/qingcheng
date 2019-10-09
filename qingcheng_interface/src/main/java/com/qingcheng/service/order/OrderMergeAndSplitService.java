package com.qingcheng.service.order;

import com.qingcheng.pojo.order.OrderItem;

import java.util.List;

public interface OrderMergeAndSplitService {
    public void OrderMerge(Integer OrderId1,Integer OrderId2);
    public void OrderSplit(List<OrderItem> orderItemList);
}
