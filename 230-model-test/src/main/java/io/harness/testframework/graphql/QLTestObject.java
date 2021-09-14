/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.testframework.graphql;

import io.harness.exception.UnexpectedException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLTestObject {
  private LinkedHashMap map;
  private ArrayList arr;

  public Object get(int index) {
    return arr.get(index);
  }

  public Object get(String path) {
    return map.get(path);
  }

  public int size() {
    if (map != null) {
      return map.size();
    }
    if (arr != null) {
      return arr.size();
    }

    throw new UnexpectedException();
  }
  public QLTestObject sub(int index) {
    final Object value = arr.get(index);
    if (value instanceof LinkedHashMap) {
      return QLTestObject.builder().map((LinkedHashMap) value).build();
    }
    if (value instanceof ArrayList) {
      return QLTestObject.builder().arr((ArrayList) value).build();
    }

    throw new UnexpectedException();
  }

  public QLTestObject sub(String path) {
    final Object value = map.get(path);
    if (value instanceof LinkedHashMap) {
      return QLTestObject.builder().map((LinkedHashMap) value).build();
    }
    if (value instanceof ArrayList) {
      return QLTestObject.builder().arr((ArrayList) value).build();
    }

    throw new UnexpectedException();
  }
}
