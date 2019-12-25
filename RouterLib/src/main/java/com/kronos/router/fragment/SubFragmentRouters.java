package com.kronos.router.fragment;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SubFragmentRouters {
    int fragmentType();
    String widgetIdName() default "";
    String filedName() default "";
}
