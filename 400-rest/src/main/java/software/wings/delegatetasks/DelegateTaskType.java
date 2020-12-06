package software.wings.delegatetasks;

import software.wings.beans.TaskType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by peeyushaggarwal on 1/12/17.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DelegateTaskType {
  TaskType value();
}
