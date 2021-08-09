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
