/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("B")
public class TestJsonB extends TestJsonBase {
  private String name = TestJsonB.class.getName();

  public TestJsonB() {
    setBaseType(BaseType.B);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
