package software.wings.rules;

import org.mockito.Mock;
import software.wings.core.queue.AbstractQueueListener;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by peeyushaggarwal on 5/11/16.
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD, ElementType.TYPE})
public @interface Listeners {
  Class<? extends AbstractQueueListener>[] value() default {};
}
