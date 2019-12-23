package com.kronos.router.fragment;

public @interface SubFragmentRouters {
    int fragmentType();
    int widgetId() default -1;
    String filedName() default "";
}
