package com.siyeh.igtest.verbose;

public class PointlessBitwiseExpressionInspection {
    public static void main(String[] args) {
        final int i = 1;
        int j = i & 0;
        System.out.println(j);
        j = 3;
        int k = j | 0;
        System.out.println(k);
        k = j ^ 0;
        System.out.println(k);
        k = j << 0;
        System.out.println(k);
        k = j >> 0;
        System.out.println(k);
        k = j >>> 0;
        System.out.println(k);
    }

    public static void main3(String[] args) {
        final int i = 1;
        int j = 0 & i;
        System.out.println(j);
        j = 3;
        int k = 0 | j;
        System.out.println(k);
        k = 0 ^ j;
        System.out.println(k);

    }

    public static void main2(String[] args) {
        final int i = 1;
        int j = i & 0xffffffff;
        System.out.println(j);
        j = 3;
        int k = j | 0xffffffff;
        System.out.println(k);
        j = 6;
        k = j ^ 0xffffffff;
        System.out.println(k);


    }

    public static void main4(String[] args) {
        final int i = 1;
        int j = 0xffffffff & i;
        System.out.println(j);
        j = 3;
        int k = 0xffffffff | j;
        System.out.println(k);
        j = 6;
        k = 0xffffffff ^ j;
        System.out.println(k);


    }
}
