package com.shop.service;

import com.shop.dto.OrderDTO;
import com.shop.dto.OrderHistDTO;
import com.shop.dto.OrderItemDTO;
import com.shop.entity.*;
import com.shop.repository.ItemImgRepository;
import com.shop.repository.ItemRepository;
import com.shop.repository.MemberRepository;
import com.shop.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.aspectj.weaver.ast.Or;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.util.StringUtils;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

    private final ItemRepository itemRepository;
    private final MemberRepository memberRepository;
    private final OrderRepository orderRepository;
    private final ItemImgRepository itemImgRepository;

    public Long order(OrderDTO orderDTO, String email){
        Item item = itemRepository.findById(orderDTO.getItemId())
                    .orElseThrow(EntityExistsException::new);
        Member member = memberRepository.findByEmail(email);

        List<OrderItem> orderItemList = new ArrayList<>();
        OrderItem orderItem = OrderItem.createOrderItem(item, orderDTO.getCount());
        orderItemList.add(orderItem);

        Order order = Order.createOrder(member, orderItemList);
        orderRepository.save(order);

        return order.getId();
    }

    @Transactional(readOnly = true)
    public Page<OrderHistDTO> getOrderList(String email, Pageable pageable){
        List<Order> orders = orderRepository.findOrders(email, pageable);
        Long totalCount = orderRepository.countOrder(email);

        List<OrderHistDTO> orderHistDTOs = new ArrayList<>();

        for(Order order : orders){
            OrderHistDTO orderHistDTO = new OrderHistDTO(order);
            List<OrderItem> orderItems = order.getOrderItems();
            for(OrderItem orderItem : orderItems){
                ItemImg itemImg = itemImgRepository.findByItemIdAndRepimgYn(orderItem.getItem().getId(), "Y");
                OrderItemDTO orderItemDTO = new OrderItemDTO(orderItem, itemImg.getImgUrl());
                orderHistDTO.addOrderItemDTO(orderItemDTO);
            }
            orderHistDTOs.add(orderHistDTO);
        }
        return new PageImpl<OrderHistDTO>(orderHistDTOs, pageable, totalCount);
    }

    @Transactional(readOnly = true)
    public boolean validateOrder(Long orderId, String email){
        Member curMember = memberRepository.findByEmail(email);
        Order order = orderRepository.findById(orderId).orElseThrow(EntityNotFoundException::new);
        Member savedMember = order.getMember();

        if(!StringUtils.equals(curMember.getEmail(), savedMember.getEmail())){
            return false;
        }

        return true;
    }

    public void cancelOrder(Long orderId){
        Order order = orderRepository.findById(orderId).orElseThrow(EntityNotFoundException::new);
        order.cancelOrder();
    }

    public Long orders(List<OrderDTO> orderDTOList, String email){
        Member member = memberRepository.findByEmail(email);
        List<OrderItem> orderItemList = new ArrayList<>();

        for(OrderDTO orderDTO : orderDTOList){
            Item item = itemRepository.findById(orderDTO.getItemId())
                    .orElseThrow(EntityNotFoundException::new);
            OrderItem orderItem = OrderItem.createOrderItem(item, orderDTO.getCount());
            orderItemList.add(orderItem);
        }

        Order order = Order.createOrder(member, orderItemList);
        orderRepository.save(order);

        return order.getId();


    }



}
