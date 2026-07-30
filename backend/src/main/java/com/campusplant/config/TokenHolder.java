package com.campusplant.config;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存 Token 存储（Phase 1 简易方案）。
 * Phase 2 可替换为 Redis 或数据库存储。
 */
public class TokenHolder {
    private static final Set<String> TOKENS = ConcurrentHashMap.newKeySet();

    public static void add(String token) {
        TOKENS.add(token);
    }

    public static boolean contains(String token) {
        return TOKENS.contains(token);
    }

    public static void remove(String token) {
        TOKENS.remove(token);
    }
}
