package com.kronos.sample;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import jaygoo.library.m3u8downloader.M3U8Downloader;

public class M3u8C {
    public static M3U8Downloader downloader;

    static {
        try {
            downloader = M3U8Downloader.getInstance();
//            Constructor[] constructors = M3U8Downloader.class.getDeclaredConstructors();
//            if(constructors != null && constructors.length > 0){
//                for (Constructor constructor : constructors) {
//                     Class[] cls  = constructor.getParameterTypes();
//                    if(cls == null || cls.length == 0){
//                        constructor.setAccessible(true);
//                        downloader = (M3U8Downloader) constructor.newInstance(null);
//                    }
//                }
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
