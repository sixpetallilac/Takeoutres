package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.ast.Or;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Override
    @Transactional
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {
        //->简单逻辑异常判断
        //AddressBook ID判断地址簿是否为null
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (null == addressBook){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        //购物车null判断
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if (list == null || list.size() == 0){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //->插入数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,orders);
        //补充dto没有的固定数据
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));//订单号简单实现
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        //address->str
        String address = addressBook.getProvinceName()+" "+addressBook.getCityName()+" "+addressBook.getDistrictName()+" "+addressBook.getDetail();
        orders.setAddress(address);
        orders.setUserId(userId);
        //mapper
        orderMapper.insert(orders);
        List<OrderDetail> orderDetailList = new ArrayList<>();
        //订单详细数据 iterator cart
        for (ShoppingCart cart: list){
            //OrderDetail 中的属性 == ShoppingCart，统一list insert
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart,orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);

        //订单提交后清空购物车
        shoppingCartMapper.clean(userId);

        //封装vo结果 as return
        OrderSubmitVO orderSubmitVO = OrderSubmitVO
                .builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();
        return orderSubmitVO;
    }

    /**
     * 订单支付
     * 没有商户号，仅供参考
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    /**
     * 历史订单查询 -- 分页 result -> vo list返回
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult historyOrders(OrdersPageQueryDTO ordersPageQueryDTO) {
        //orders pages get
        PageHelper.startPage(ordersPageQueryDTO.getPage(),ordersPageQueryDTO.getPageSize());
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);
        //details get
        List<OrderVO> list = new ArrayList<>();
        if (page != null && page.getTotal()>0){

            for (Orders orders:page){
                Long ordersId = orders.getId();
                List<OrderDetail> details = orderDetailMapper.getByOrderId(ordersId);
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders,orderVO);
                orderVO.setOrderDetailList(details);
                //vo list作为result
                list.add(orderVO);
            }
        }
        return new PageResult(page.getTotal(),list);
    }

    /**
     * 查询订单详情
     * @param id
     * @return
     */
    @Override
    public OrderVO orderDetail(Long id) {
        //ordervo extends order 所以需要查询一下order
        Orders orders = orderMapper.getById(id);
        Long id1 = orders.getId();
        List<OrderDetail> details = orderDetailMapper.getByOrderId(id);
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders,orderVO);
        orderVO.setOrderDetailList(details);
        return orderVO;
    }

    /**
     * 取消订单
     * @param orderId
     * @throws Exception
     */
    @Override
    public void cancelOrder(Long orderId) throws Exception {
        //status订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消 7退款
        //payStatus支付状态 0未支付 1已支付 2退款
//        - 待支付和待接单状态下，用户可直接取消订单 if status2 && paystatus 0 mapperupdate
//                - 商家已接单状态下，用户取消订单需电话沟通商家 if 3
//                - 派送中状态下，用户取消订单需电话沟通商家
//                - 如果在待接单状态下取消订单，需要给用户退款
//                - 取消订单后需要将订单状态修改为“已取消”
        //查询
        Orders orders = orderMapper.getById(orderId);
        if (null == orders){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //status判断
        if (orders.getStatus()>2){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders updateOrder = Orders.builder()
                .id(orders.getId())
                .status(Orders.CANCELLED)
                .cancelReason("用户取消")
                .cancelTime(LocalDateTime.now())
                .build();
        if (orders.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            //exception wechat 退款
            updateOrder.setPayStatus(Orders.REFUND);
            log.info("订单号{},订单价格{}",orders.getNumber(),orders.getAmount());
            //没有商户号此处默认set执行paystatus
//            weChatPayUtil.refund(
//                    orders.getNumber(),
//                    orders.getNumber(),
//                    orders.getAmount(),
//                    orders.getAmount());
        }
        orderMapper.update(updateOrder);
    }

    /**
     * 再来一单
     * @param orderId
     */
    @Override
    public void repetition(Long orderId) {
        //订单菜品相关都在detail中，detail和cart几乎相似，所以可以做转化，操作上也符合逻辑
        Long userId = BaseContext.getCurrentId();
        List<OrderDetail> details = orderDetailMapper.getByOrderId(orderId);
        List<ShoppingCart> carts = details.stream().map(detail -> {
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(detail, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());
            return shoppingCart;
        }).collect(Collectors.toList());
        //加进购物车
        shoppingCartMapper.insertBatch(carts);
    }

    /**
     * 订单条件查询
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult orderSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(),ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);
        return new PageResult(page.getTotal(),resultList(page));
    }
    //basic ✔
    //utils combine
    private List<OrderVO> resultList(Page<Orders> page){
        List<Orders> result = page.getResult();
        //返回值为vo 需要新建arrlist
        List<OrderVO> voList = new ArrayList<>();

        if (!CollectionUtils.isEmpty(result)){//Collectionutils可以直接判断null那些问题，省工作量
            for (Orders orders:result){
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders,orderVO);
                //查出来所有details，vo中的冗余字段赋值（把details名称数量信息混合在一个str中）
                List<OrderDetail> details = orderDetailMapper.getByOrderId(orders.getId());
                List<String> collect = details.stream().map(x -> {
                    String orderDishes = x.getName() + "*" + x.getNumber();
                    return orderDishes;
                }).collect(Collectors.toList());
                String orderDishes = String.join(",", collect);
                //设置冗余字段值
                orderVO.setOrderDishes(orderDishes);
                //add至volist
                voList.add(orderVO);
            }
        }
        //vo作为返回值
        return voList;
    }

    /**
     * 各个状态的订单数量统计
     * @return
     */
    @Override
    public OrderStatisticsVO statistics() {
        OrderStatisticsVO statisticsVO = new OrderStatisticsVO();
        statisticsVO.setConfirmed(orderMapper.statistics(Orders.CONFIRMED));
        statisticsVO.setToBeConfirmed(orderMapper.statistics(Orders.TO_BE_CONFIRMED));
        statisticsVO.setDeliveryInProgress(orderMapper.statistics(Orders.DELIVERY_IN_PROGRESS));
        return statisticsVO;
    }

}
