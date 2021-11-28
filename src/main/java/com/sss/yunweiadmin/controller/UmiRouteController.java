package com.sss.yunweiadmin.controller;

import com.sss.yunweiadmin.model.entity.SysUser;
import com.sss.yunweiadmin.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpSession;

@Controller
public class UmiRouteController {
    @Autowired
    SysUserService sysUserService;
    @Autowired
    HttpSession httpSession;

    @GetMapping("/redirect")
    public String redirect() {
        //
        return "redirect:/ssologin";
    }

    @GetMapping("/ssologin")
    public String ssologin() {
        //单点登录代码
        //用户放入session
        SysUser user = sysUserService.getById(19);
        httpSession.removeAttribute("user");
        httpSession.setAttribute("user", user);
        //
        return "index";
    }

    //umirc.ts -> routes -> path
    @GetMapping({"/login", "/back"})
    public String route() {
        System.out.println("route");
        return "index";
    }
}
