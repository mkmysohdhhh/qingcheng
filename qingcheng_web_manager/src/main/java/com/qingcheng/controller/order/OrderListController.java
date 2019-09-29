package com.qingcheng.controller.order;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.pojo.order.OrderList;
import com.qingcheng.service.order.OrderListService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/orderList")
public class OrderListController {

    @Reference
    private OrderListService orderListService;

    @PostMapping("/getList")
    public List<OrderList> getList(@RequestBody String[] ids) {
//        System.out.println(Arrays.asList(ids));
        if (ids.length > 0) {
            List<OrderList> list = orderListService.getList(ids);
            return list;
        } else {
            throw  new RuntimeException("没有接收到ID");
        }
    }
}
