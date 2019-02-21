package io.harness.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An marker annotation for natural key fields in exportable harness entity classes. All the natural key
 * fields will help uniquely identify an entity other than the '_id' UUID. This will help identify potential
 * data clashes - those have different '_id' values but with the same 'natural key'.
 *
 * @author marklu on 2019-02-20
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface NaturalKey {}
