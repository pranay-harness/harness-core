package io.harness.ccm.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use this annotation when your method has a string argument annotated with @AccountIdentifier and you want to add
 * accountId to the log context
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface LogAccountIdentifier {}
