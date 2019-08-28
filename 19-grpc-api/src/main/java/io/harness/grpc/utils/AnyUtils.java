package io.harness.grpc.utils;

import com.google.protobuf.Any;
import com.google.protobuf.Message;

import lombok.experimental.UtilityClass;

/**
 * To get the java classes from Any
 */
@UtilityClass
public class AnyUtils {
  /**
   * Get the fully qualified class name for the Any payload.
   */
  public String toFqcn(Any any) {
    return any.getTypeUrl().split("/")[1];
  }

  /**
   * Get the class type for the Any payload.
   */
  public Class<? extends Message> toClass(Any any) throws ClassNotFoundException {
    @SuppressWarnings("unchecked") // Any can only contain Message
    Class<? extends Message> clazz = (Class<? extends Message>) Class.forName(toFqcn(any));
    return clazz;
  }
}
