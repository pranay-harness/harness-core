package io.harness.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A marker annotation for exportable Harness entity classes. Once annotated the account export process
 * will try exporting records associated with the specified account from the mongo collections associated with
 * this entity.
 *
 * For any entity classes with this annotation, it should either have a 'accountId' field or an 'appId' field.
 * Will add an compile time validation to enforce this constraint.
 *
 * @author marklu on 2019-02-20
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface HarnessExportableEntity {}
