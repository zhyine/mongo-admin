package com.verdure.mongodb.admin.utils;

import com.mongodb.MongoClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author verdure
 * @date 2021/5/13 9:41 下午
 */

public class Constant {

 /**
  * 需要过滤的表
  */
 public final static String[] TAVLEARR = {"system.indexes"};

 public  static Map<String, Map<String, String>> CONNECTIONSCACHE = new HashMap<>();

 public final static List<String> CACHE = new ArrayList<>();

 public static String ACTIVEHOST = "127.0.0.1";

 public static Map<String, MongoClient> HOSTOMONGOCLIENT = new HashMap<String, com.mongodb.MongoClient>();
}