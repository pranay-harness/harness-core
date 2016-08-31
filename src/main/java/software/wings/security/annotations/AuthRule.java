package software.wings.security.annotations;

import software.wings.security.PermissionAttribute.PermissionScope;
import software.wings.security.PermissionAttribute;

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
   * Value string.
   *
   * @return the string
   */
  String[] value() default {}; /* Resource:Action */
  PermissionScope scope() default PermissionAttribute.PermissionScope.APP;
}
