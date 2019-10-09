package com.qingcheng.controller.order;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.entity.Result;
import com.qingcheng.pojo.order.OrderItem;
import com.qingcheng.service.order.OrderMergeAndSplitService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/OrderOperation")
public class OrderMergeAndSplitController {
    @Reference
    private OrderMergeAndSplitService orderMergeAndSplitService;

    @RequestMapping("/merge")
    public Result merge(Integer OrderId1, Integer OrderId2) {
        orderMergeAndSplitService.OrderMerge(OrderId1, OrderId2);
        return new Result();
    }

    @RequestMapping("/split")
    public Result split(List<OrderItem> orderItemList) {
        if (orderItemList.size() > 0) {
            orderMergeAndSplitService.OrderSplit(orderItemList);
            return new Result();
        }
        throw new RuntimeException("未接收到ID数组");
    }
}
