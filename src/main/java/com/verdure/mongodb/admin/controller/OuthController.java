package com.verdure.mongodb.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author verdure
 * @date 2021/5/25 12:10 下午
 */

@Controller
public class OuthController {

    @RequestMapping("/")
    public String index() {
        return "redirect:/oauth/render";
    }
}