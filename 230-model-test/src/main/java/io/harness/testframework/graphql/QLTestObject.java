/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
