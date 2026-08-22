package com.agrodairy.management.util;

public class PasswordGenerator {
    public static void main(String[] args) {
        System.out.println(PasswordUtil.hashPassword("secret123"));
    }
}