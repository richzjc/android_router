package com.kronos.router.fragment;

public @interface ISubFragmentRouters {
    int fragmentType();
    int widgetId() default -1;
    String filedName() default "";
}
