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
