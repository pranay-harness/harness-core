/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.expression.functors;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.ExpressionFunctor;
import io.harness.serializer.JsonUtils;

import java.util.HashMap;

@OwnedBy(HarnessTeam.PIPELINE)
public class NGJsonFunctor implements ExpressionFunctor {
  public Object object(String json) {
    return JsonUtils.asObject(json, HashMap.class);
  }

  public Object select(String path, String json) {
    return JsonUtils.jsonPath(json, path);
  }

  public Object list(String path, String json) {
    return JsonUtils.jsonPath(json, path);
  }

  public String format(Object object) {
    return JsonUtils.asJson(object);
  }
}
