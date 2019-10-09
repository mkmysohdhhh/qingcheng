package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingcheng.dao.*;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.order.*;
import com.qingcheng.service.order.OrderService;
import com.qingcheng.util.IdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service(interfaceClass = OrderService.class)
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private ReturnOrderMapper returnOrderMapper;

    @Autowired
    private ReturnOrderItemMapper returnOrderItemMapper;

    @Autowired
    private OrderConfigMapper orderConfigMapper;

    @Autowired
    private OrderLogMapper orderLogMapper;

    @Autowired
    private IdWorker idWorker;
    /**
     * 返回全部记录
     *
     * @return
     */
    public List<Order> findAll() {
        return orderMapper.selectAll();
    }

    /**
     * 分页查询
     *
     * @param page 页码
     * @param size 每页记录数
     * @return 分页结果
     */
    public PageResult<Order> findPage(int page, int size) {
        PageHelper.startPage(page, size);
        Page<Order> orders = (Page<Order>) orderMapper.selectAll();
        return new PageResult<Order>(orders.getTotal(), orders.getResult());
    }

    /**
     * 条件查询
     *
     * @param searchMap 查询条件
     * @return
     */
    public List<Order> findList(Map<String, Object> searchMap) {
        Example example = createExample(searchMap);
        return orderMapper.selectByExample(example);
    }

    /**
     * 分页+条件查询
     *
     * @param searchMap
     * @param page
     * @param size
     * @return
     */
    public PageResult<Order> findPage(Map<String, Object> searchMap, int page, int size) {
        PageHelper.startPage(page, size);
        Example example = createExample(searchMap);
        Page<Order> orders = (Page<Order>) orderMapper.selectByExample(example);
        return new PageResult<Order>(orders.getTotal(), orders.getResult());
    }

    /**
     * 根据Id查询
     *
     * @param id
     * @return
     */
    public Order findById(String id) {
        return orderMapper.selectByPrimaryKey(id);
    }

    /**
     * 新增
     *
     * @param order
     */
    public void add(Order order) {
        orderMapper.insert(order);
    }

    /**
     * 修改
     *
     * @param order
     */
    public void update(Order order) {
        orderMapper.updateByPrimaryKeySelective(order);
    }

    /**
     * 删除
     *
     * @param id
     */
    public void delete(String id) {
        orderMapper.deleteByPrimaryKey(id);
    }

    /**
     * 批量发货
     *
     * @param orders
     */
    @Transactional
    @Override
    public void batchSend(List<Order> orders) {
        for (Order order : orders) {
            if (order.getShippingCode() == null || order.getShippingName() == null) {
                throw new RuntimeException("请将订单信息补充完整");
            }
            order.setOrderStatus("3");
            order.setConsignStatus("2");
            order.setConsignTime(new Date());
            orderMapper.updateByPrimaryKeySelective(order);
            //TODO 记录订单日志
        }
    }

    /**
     * 驳回退款
     *
     * @param id
     * @param remark
     * @param adminId
     */
    public void rejectRefund(String id, String remark, Integer adminId) {
        ReturnOrder returnOrder = returnOrderMapper.selectByPrimaryKey(id);
        if (returnOrder == null) {
            throw new RuntimeException("退款订单不存在！");
        }
        if (!returnOrder.getType().equals("2")) {
            throw new RuntimeException("不是退款订单！");
        }
        if (remark.length() < 5) {
            throw new RuntimeException("请输入驳回理由！");
        }
        //修改属性
        returnOrder.setRemark(remark);//驳回理由
        returnOrder.setStatus("2");//驳回
        returnOrder.setAdminId(adminId);//管理员
        returnOrder.setDisposeTime(new Date());//处理日期
        returnOrderMapper.updateByPrimaryKeySelective(returnOrder);//保存

        //修改对应订单明细的退款状态为未申请
        Example example = new Example(ReturnOrderItem.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("returnOrderId", id);//退费订单ID
        List<ReturnOrderItem> returnOrderItems = returnOrderItemMapper.selectByExample(example);
        for (ReturnOrderItem returnOrderItem : returnOrderItems) {
            OrderItem orderitem = new OrderItem();
            orderitem.setId(returnOrderItem.getOrderItemId() + "");//提取订单明细ID
            orderitem.setIsReturn("0");
            orderItemMapper.updateByPrimaryKeySelective(orderitem);//更新状态，让其可以重新发送退款
        }
    }

    /**
     * 订单超时处理
     */
    @Transactional
    @Override
    public void orderTimeOutLogic() {
        //读取配置表，获得超时时间
        OrderConfig orderConfig = orderConfigMapper.selectByPrimaryKey(1);
        Integer timeout = orderConfig.getOrderTimeout();

        //当前时间-超时时间=超时时间点
        LocalTime outTime = LocalTime.now().minusMinutes(timeout);

        //获得订单信息
        Example example = new Example(Order.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andLessThan("createTime", outTime);
        criteria.andEqualTo("isDelete", "0");
        criteria.andEqualTo("orderStatus", "0");
        List<Order> orderList = orderMapper.selectByExample(example);//超时订单列表
        if (orderList != null) {
            for (Order order : orderList) {
                //更新订单状态
                order.setOrderStatus("4");
                order.setCloseTime(new Date());
                orderMapper.updateByPrimaryKeySelective(order);

                //记录日志
                OrderLog orderLog = new OrderLog();
                orderLog.setId(idWorker.nextId()+"");
                orderLog.setOperater("system");
                orderLog.setOperateTime(new Date());
                orderLog.setOrderStatus("4");
                orderLog.setPayStatus(order.getPayStatus());
                orderLog.setConsignStatus(order.getConsignStatus());
                orderLog.setRemarks("超时订单，系统自动关闭");
                orderLog.setOrderId((long) Integer.parseInt(order.getId()));
                orderLogMapper.insertSelective(orderLog);
            }
        }
    }

    /**
     * 构建查询条件
     *
     * @param searchMap
     * @return
     */
    private Example createExample(Map<String, Object> searchMap) {
        Example example = new Example(Order.class);
        Example.Criteria criteria = example.createCriteria();
        if (searchMap != null) {
            // 订单id
            if (searchMap.get("id") != null && !"".equals(searchMap.get("id"))) {
                criteria.andLike("id", "%" + searchMap.get("id") + "%");
            }
            // 支付类型，1、在线支付、0 货到付款
            if (searchMap.get("payType") != null && !"".equals(searchMap.get("payType"))) {
                criteria.andLike("payType", "%" + searchMap.get("payType") + "%");
            }
            // 物流名称
            if (searchMap.get("shippingName") != null && !"".equals(searchMap.get("shippingName"))) {
                criteria.andLike("shippingName", "%" + searchMap.get("shippingName") + "%");
            }
            // 物流单号
            if (searchMap.get("shippingCode") != null && !"".equals(searchMap.get("shippingCode"))) {
                criteria.andLike("shippingCode", "%" + searchMap.get("shippingCode") + "%");
            }
            // 用户名称
            if (searchMap.get("username") != null && !"".equals(searchMap.get("username"))) {
                criteria.andLike("username", "%" + searchMap.get("username") + "%");
            }
            // 买家留言
            if (searchMap.get("buyerMessage") != null && !"".equals(searchMap.get("buyerMessage"))) {
                criteria.andLike("buyerMessage", "%" + searchMap.get("buyerMessage") + "%");
            }
            // 是否评价
            if (searchMap.get("buyerRate") != null && !"".equals(searchMap.get("buyerRate"))) {
                criteria.andLike("buyerRate", "%" + searchMap.get("buyerRate") + "%");
            }
            // 收货人
            if (searchMap.get("receiverContact") != null && !"".equals(searchMap.get("receiverContact"))) {
                criteria.andLike("receiverContact", "%" + searchMap.get("receiverContact") + "%");
            }
            // 收货人手机
            if (searchMap.get("receiverMobile") != null && !"".equals(searchMap.get("receiverMobile"))) {
                criteria.andLike("receiverMobile", "%" + searchMap.get("receiverMobile") + "%");
            }
            // 收货人地址
            if (searchMap.get("receiverAddress") != null && !"".equals(searchMap.get("receiverAddress"))) {
                criteria.andLike("receiverAddress", "%" + searchMap.get("receiverAddress") + "%");
            }
            // 订单来源：1:web，2：app，3：微信公众号，4：微信小程序  5 H5手机页面
            if (searchMap.get("sourceType") != null && !"".equals(searchMap.get("sourceType"))) {
                criteria.andLike("sourceType", "%" + searchMap.get("sourceType") + "%");
            }
            // 交易流水号
            if (searchMap.get("transactionId") != null && !"".equals(searchMap.get("transactionId"))) {
                criteria.andLike("transactionId", "%" + searchMap.get("transactionId") + "%");
            }
            // 订单状态
            if (searchMap.get("orderStatus") != null && !"".equals(searchMap.get("orderStatus"))) {
                criteria.andLike("orderStatus", "%" + searchMap.get("orderStatus") + "%");
            }
            // 支付状态
            if (searchMap.get("payStatus") != null && !"".equals(searchMap.get("payStatus"))) {
                criteria.andLike("payStatus", "%" + searchMap.get("payStatus") + "%");
            }
            // 发货状态
            if (searchMap.get("consignStatus") != null && !"".equals(searchMap.get("consignStatus"))) {
                criteria.andLike("consignStatus", "%" + searchMap.get("consignStatus") + "%");
            }
            // 是否删除
            if (searchMap.get("isDelete") != null && !"".equals(searchMap.get("isDelete"))) {
                criteria.andLike("isDelete", "%" + searchMap.get("isDelete") + "%");
            }

            // 数量合计
            if (searchMap.get("totalNum") != null) {
                criteria.andEqualTo("totalNum", searchMap.get("totalNum"));
            }
            // 金额合计
            if (searchMap.get("totalMoney") != null) {
                criteria.andEqualTo("totalMoney", searchMap.get("totalMoney"));
            }
            // 优惠金额
            if (searchMap.get("preMoney") != null) {
                criteria.andEqualTo("preMoney", searchMap.get("preMoney"));
            }
            // 邮费
            if (searchMap.get("postFee") != null) {
                criteria.andEqualTo("postFee", searchMap.get("postFee"));
            }
            // 实付金额
            if (searchMap.get("payMoney") != null) {
                criteria.andEqualTo("payMoney", searchMap.get("payMoney"));
            }

        }
        return example;
    }

}
