package com.kronos.router.utils;

import android.content.Context;

import java.lang.reflect.Field;

public class ReflectUtil {
    public static int getId(Context context, String defaultType, String resourceName){
        try {
            if(resourceName.startsWith("R")){
                String[] spl = resourceName.split("\\.");
                if(spl != null && spl.length == 3){
                    return getRealId(context, spl[1], spl[2]);
                }else{
                    return getRealId(context, defaultType, resourceName);
                }
            }else{
               return getRealId(context, defaultType, resourceName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static int getRealId(Context context, String type, String resourceName) throws Exception {
        StringBuilder builder = new StringBuilder();
        builder.append(context.getPackageName())
                .append(".R$")
                .append(type);
        Class cls = Class.forName(builder.toString());
        Field field = cls.getDeclaredField(resourceName);
        field.setAccessible(true);
        return (int) field.get(null);
    }
}
