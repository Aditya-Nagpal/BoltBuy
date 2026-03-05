package com.boltbuy.orderservice.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {
    /**
     * Key expiration time in seconds. Set to 1 hour.
     */
    long expire() default 3600;

    /**
     * Custom message for duplicate requests.
     */
    String message() default "Request is being processed or already completed.";
}