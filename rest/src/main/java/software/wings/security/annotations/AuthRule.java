package software.wings.security.annotations;

import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.ws.rs.NameBinding;

/**
 * Created by anubhaw on 3/10/16.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@NameBinding
public @interface AuthRule {
  /**
   * Scope permission permissionType.
   *
   * @return the permission permissionType
   */
  PermissionType permissionType() default PermissionType.NONE;

  Action action() default Action.DEFAULT;

  /**
   * If the query / path parameter has a different name other than the default name for the permission type.
   * @return
   */
  String parameterName() default "";

  boolean skipAuth() default false;

  String dbFieldName() default "";

  String dbCollectionName() default "";
}
