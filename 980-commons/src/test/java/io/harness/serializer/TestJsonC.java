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
