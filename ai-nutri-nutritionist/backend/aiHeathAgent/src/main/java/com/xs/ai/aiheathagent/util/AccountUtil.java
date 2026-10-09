package com.xs.ai.aiheathagent.util;

import java.util.*;
import java.util.regex.Pattern;

public class AccountUtil {

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // 通用 E.164：+开头可选，后接 6~20 位数字
    private static final Pattern E164_GENERIC = Pattern.compile("^\\+?\\d{6,20}$");

    // 大陆手机：(+86)? 1[3-9] 开头共 11 位
    private static final Pattern CN_MOBILE = Pattern.compile("^(?:\\+?86)?1[3-9]\\d{9}$");

    // 台湾手机：(+886)? 9 开头共 9 位（本地写法 09XXXXXXXX 共10位）
    private static final Pattern TW_MOBILE_E164 = Pattern.compile("^(?:\\+?886)?9\\d{8}$");
    private static final Pattern TW_MOBILE_LOCAL = Pattern.compile("^09\\d{8}$");

    // 可选：你的 UID 规则（按需改）
    private static final Pattern UID = Pattern.compile("^[A-Za-z0-9_\\-]{4,32}$");

    /** 是否邮箱 */
    public static boolean isEmail(String s) {
        String x = normalizeAccount(s);
        return x != null && EMAIL.matcher(x).matches();
    }

    /** 是否手机号（支持 +86 / +886 / 通用 E.164） */
    public static boolean isPhone(String s) {
        String x = normalizePhone(s);
        if (x == null) return false;
        // 大陆、台湾优先命中；否则用通用 E.164 兜底
        return CN_MOBILE.matcher(x).matches()
                || TW_MOBILE_E164.matcher(x).matches()
                || E164_GENERIC.matcher(x).matches();
    }

    /** 是否像 UID（当既不是邮箱也不是手机号时，用它兜底） */
    public static boolean isUid(String s) {
        String x = normalizeAccount(s);
        return x != null && UID.matcher(x).matches();
    }

    /** 去掉空白、全角空格、常见分隔符，统一大小写 */
    public static String normalizeAccount(String s) {
        if (s == null) return null;
        String x = s.trim()
                .replaceAll("[\\u3000\\s]+", "")       // 全角/半角空白
                .replaceAll("[()\\[\\]{}.-]", "")      // 常见分隔
                .replaceAll("\u00A0", "");             // 不换行空格
        // 邮箱保留点号和@，所以对邮箱时不要用上面的点号去除；做一个简单的分支
        if (s.contains("@")) {
            x = s.trim();
        }
        return x;
    }

    /** 规范化手机号：去空白/分隔符；把 00 开头转为 +；保留 + 前缀 */
    public static String normalizePhone(String s) {
        if (s == null) return null;
        String x = s.trim()
                .replaceAll("[\\s\\-_.()\\u3000]", ""); // 去空白及常见分隔
        if (x.startsWith("00")) x = "+" + x.substring(2);
        // 台湾本地 09XXXXXXXX 转 E.164：+8869XXXXXXXX
        if (TW_MOBILE_LOCAL.matcher(x).matches()) {
            x = "+886" + x.substring(1);
        }
        return x;
    }

    /**
     * 数据库查询时的“手机号候选”。为兼容库里有的存 +86，有的存裸 11 位：
     * - 若是 +86 开头，返回 [原样, 去掉 +86]
     * - 若是 11 位大陆号，返回 [原样, +86 + 原样]
     * - 若是 +886 开头，返回 [原样, 去掉 +886 前缀后补 0 形成本地 09… 也可按需加入]
     */
    public static List<String> phoneQueryVariants(String s) {
        String n = normalizePhone(s);
        if (n == null) return List.of();
        Set<String> set = new LinkedHashSet<>();
        set.add(n);

        // +86 与裸号互转
        if (n.startsWith("+86") && n.length() == 14) {      // +8613xxxxxxxxx
            set.add(n.substring(3));                        // 13xxxxxxxxx
        } else if (n.matches("^1[3-9]\\d{9}$")) {           // 裸 11 位
            set.add("+86" + n);
        }

        // +886 与本地 09…互转（按你库里存储习惯选用）
        if (n.startsWith("+8869") && n.length() == 13) {    // +8869xxxxxxxx
            set.add("0" + n.substring(4));                  // 09xxxxxxxx
        } else if (TW_MOBILE_LOCAL.matcher(n).matches()) {
            set.add("+886" + n.substring(1));
        }

        return new ArrayList<>(set);
    }
}
