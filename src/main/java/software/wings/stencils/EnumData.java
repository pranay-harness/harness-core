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
  boolean expandIntoMultipleEntries() default false;

  Class<? extends DataProvider> enumDataProvider();
}
