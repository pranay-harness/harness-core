/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.expression;

import io.harness.serializer.JsonUtils;

import java.util.HashMap;
import java.util.LinkedList;

public class JsonFunctor implements ExpressionFunctor {
  public Object object(String json) {
    return JsonUtils.asObject(json, HashMap.class);
  }

  public Object select(String path, String json) {
    final Object object = JsonUtils.jsonPath(json, path);
    if (object instanceof String) {
      return object;
    } else if (object instanceof LinkedList) {
      LinkedList list = (LinkedList) object;
      if (list.size() == 1) {
        return list.get(0);
      }
    }
    return null;
  }

  public Object list(String path, String json) {
    return JsonUtils.jsonPath(json, path);
  }

  public String format(Object object) {
    return JsonUtils.asJson(object);
  }
}
