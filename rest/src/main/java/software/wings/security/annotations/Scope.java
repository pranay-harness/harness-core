package software.wings.security.annotations;

import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.PermissionType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.ws.rs.NameBinding;

/**
 * @author rktummala on 3/9/18
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@NameBinding
public @interface Scope {
  /**
   * Value string.
   *
   * @return the string
   */
  PermissionAttribute.ResourceType[] value() default {}; /* Resource */

  PermissionType scope() default PermissionType.NONE;
}
