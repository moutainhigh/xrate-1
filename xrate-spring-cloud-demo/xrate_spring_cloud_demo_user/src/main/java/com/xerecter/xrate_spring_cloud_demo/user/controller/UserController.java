package com.xerecter.xrate_spring_cloud_demo.user.controller;


import com.xerecter.xrate_spring_cloud_demo.user.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author xdd
 * @since 2019-11-09
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    IUserService userService;

    @PostMapping("/minusUserBalance")
    public Boolean minusUserBalance(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "amounts", required = false) Double amounts
    ) {
        return userService.minusUserBalance(userId, amounts);
    }

}

