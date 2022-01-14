package io.harness.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A marker annotation for Harness entity classes.
 *
 * If the 'exportable' attribute is true, the account migration process will try exporting records associated with the
 * specified account from the mongo collections associated with this entity. For any entity classes annotated with
 * exportable, it should either have a 'accountId' field or an 'appId' field.
 *
 * If the 'exportable' attribute is false, it won't participate in the account migration. Some entities such as
 * deployment history, instance stats, ephemeral lock, delegate tasks etc. should fall into this category.
 *
 * @author marklu on 9/27/19
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface HarnessEntity {
  boolean exportable();
}
