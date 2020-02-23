package com.pinyougou.cart.controller;

import com.alibaba.fastjson.JSON;
import com.pinyougou.cart.service.CartService;
import com.pinyougou.common.util.CookieUtil;
import com.pinyougou.pojo.Cart;
import entity.Result;
import jdk.nashorn.internal.ir.annotations.Reference;
import org.apache.commons.lang.StringUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author ysl
 * @Date 2020/2/23 16:30
 * @Description:
 **/

@RestController
@RequestMapping("/cart")
public class CartController {

    @Reference
    private CartService cartService;


    /**
     * 获取购物车列表
     * @param request
     * @return
     */
    @RequestMapping("/findCartList")
    public List<Cart> findListCart(HttpServletRequest request){
        //获取登录人账户，判断当前是否有人登陆
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        //如果是匿名用户，则操作cookie
        if ("anonymouseUser".equals(username)){
            String cartLsitStriing = CookieUtil.getCookieValue(request, "cartLsit","UTF-8");
            if (StringUtils.isEmpty(cartLsitStriing)){
                cartLsitStriing = "[]";
            }

            List<Cart> cookieCartList = JSON.parseArray(cartLsitStriing, Cart.class);
            return cookieCartList;
        }else {
            //否则操作redis
            List<Cart> cartListFormRedis = cartService.findCartListFormRedis(username);
            return cartListFormRedis==null ? new ArrayList<>():cartListFormRedis;
        }




    }



    /**
     * 添加商品到已有的购物车的列表中
     *
     * @param itemId
     * @param num
     * @return
     */
    @RequestMapping("/addGoodsToCartList")
    public Result addGoodsToCartList(Long itemId, Integer num, HttpServletRequest request, HttpServletResponse response) {


        try {
            //获取登录人账户，判断当前是否有人登陆
            String username = SecurityContextHolder.getContext().getAuthentication().getName();

            //判断是否为匿名用户
            if ("anonymouseUser".equals(username)){
                List<Cart>cartList =findListCart(request);//获取购物车列表
                cartList = cartService.addGoodsToCartList(cartList, itemId, num);
                CookieUtil.setCookie(request, response, "cartList", JSON.toJSONString(cartList),3600*24,"UTF-8");
                return new Result(true, "添加成功");
            }else {
                //获取购物车数据
                List<Cart> cartListFormRedis = cartService.findCartListFormRedis(username);
                //向已有的购物车中添加商品
                List<Cart> cartListnew = cartService.addGoodsToCartList(cartListFormRedis,itemId,num);
                //保存最新列表到redis中
                cartService.saveCartListToRedis(username,cartListnew);
                return new Result(true,"保存成功");
            }


        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false, "添加失败");
        }
    }
}
