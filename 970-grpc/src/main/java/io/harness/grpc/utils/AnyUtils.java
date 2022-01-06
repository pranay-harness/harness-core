/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.grpc.utils;

import io.harness.exception.DataFormatException;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
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

  /**
   * Wrap Any::unpack, suppressing the checked exception.
   */
  public <T extends Message> T unpack(Any any, Class<T> clazz) {
    try {
      return any.unpack(clazz);
    } catch (InvalidProtocolBufferException e) {
      throw new DataFormatException("Unable to parse as valid protobuf", e);
    }
  }

  public <T extends Message> T findClassAndUnpack(Any any) {
    try {
      return (T) unpack(any, toClass(any));
    } catch (ClassNotFoundException e) {
      throw new DataFormatException("Unable to parse as valid protobuf", e);
    }
  }
}
