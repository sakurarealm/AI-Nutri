package com.xs.ai.aiheathagent.util;


import java.util.UUID;


public class Ids {
    public static String uid(){ return UUID.randomUUID().toString().replace("-", ""); }
}