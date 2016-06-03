package software.wings.security.annotations;

import software.wings.security.PermissionAttr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.ws.rs.NameBinding;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 3/10/16.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@NameBinding
public @interface AuthRule {
  /**
   * Value.
   *
   * @return the permission attr[]
   */
  PermissionAttr[] value() default {};
}
