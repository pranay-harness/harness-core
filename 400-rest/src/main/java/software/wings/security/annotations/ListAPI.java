package software.wings.security.annotations;

import software.wings.security.PermissionAttribute.ResourceType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by anubhaw on 8/24/16.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ListAPI {
  /**
   * Value resource type.
   *
   * @return the resource type
   */
  ResourceType value();
}
