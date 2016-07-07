package software.wings.stencils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by peeyushaggarwal on 6/3/16.
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.FIELD})
public @interface EnumData {
  /**
   * Expand into multiple entries boolean.
   *
   * @return the boolean
   */
  boolean expandIntoMultipleEntries() default false;

  /**
   * Enum data provider class.
   *
   * @return the class
   */
  Class<? extends DataProvider> enumDataProvider();
}
