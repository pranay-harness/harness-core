package io.harness.serializer;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;

@JsonTypeName("A")
@Attributes(title = "BaseA")
public class TestJsonA extends TestJsonBase {
  @Attributes(required = true) private String name = TestJsonA.class.getName();

  public TestJsonA() {
    setBaseType(BaseType.A);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
