package com.kronos.router.fragment;

public @interface SubFragmentRouters {
    int fragmentType();
    String widgetIdName() default "";
    String filedName() default "";
}
