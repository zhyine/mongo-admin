package com.verdure.mongodb.admin.utils;

import java.util.HashMap;


/**
 * @author verdure
 * @date 2021/5/9 下午5:14
 */

public class Response extends HashMap<String, Object> {

    public Response() {
        put("code", 0);
    }

    public static Response OK() {
        return new Response();
    }

    public static Response OK(String msg) {
        Response response = new Response();
        response.put("msg", msg);
        return response;
    }

    public static Response OK(int code, String msg) {
        Response response = new Response();
        response.put("code", code);
        response.put("msg", msg);
        return response;
    }

    public static Response ERR() {
        return ERR(500, "请求失败，请稍后再试");
    }

    public static Response ERR(String msg) {
        return ERR(500, msg);
    }

    public static Response ERR(int code, String msg) {
        Response response = new Response();
        response.put("code", code);
        response.put("msg", msg);
        return response;
    }


    @Override
    public Response put(String key, Object value) {
        super.put(key, value);
        return this;
    }

}