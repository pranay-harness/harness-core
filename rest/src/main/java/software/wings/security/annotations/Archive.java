package software.wings.security.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by anubhaw on 8/24/16.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Archive {
  /**
   * milliseconds after which the values will be archived
   *
   * @return the resource type
   */
  long retentionMills() default - 1;
}
