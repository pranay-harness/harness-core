/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.serializer;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.github.reinert.jjschema.SchemaIgnore;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "baseType", include = JsonTypeInfo.As.EXISTING_PROPERTY)
public class TestJsonBase {
  private BaseType baseType;

  @SchemaIgnore private String x;

  public BaseType getBaseType() {
    return baseType;
  }

  public void setBaseType(BaseType baseType) {
    this.baseType = baseType;
  }

  @SchemaIgnore
  public String getX() {
    return x;
  }

  @SchemaIgnore
  public void setX(String x) {
    this.x = x;
  }

  public enum BaseType { A, B, C }
}
