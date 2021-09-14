/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("C")
public class TestJsonC extends TestJsonBase {
  private String name = TestJsonC.class.getName();

  public TestJsonC() {
    setBaseType(BaseType.C);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
