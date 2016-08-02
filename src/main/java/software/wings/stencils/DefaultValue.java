package software.wings.stencils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by peeyushaggarwal on 7/22/16.
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.FIELD, ElementType.METHOD})
public @interface DefaultValue {
  /**
   * Value string.
   *
   * @return the string
   */
  String value();
}
