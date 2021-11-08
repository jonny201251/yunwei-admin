package com.sss.yunweiadmin.controller;

import org.springframework.stereotype.Controller;

@Controller
public class UmiRouteController {
    //umirc.ts -> routes -> path
//    @GetMapping({"/login", "/back"})
    public String route() {
        //resources -> templates -> index.html
        return "index";
    }
}
