package software.wings.helpers.ext;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "baseType")
@JsonSubTypes({
  @Type(value = BaseA.class, name = "A"), @Type(value = BaseB.class, name = "B"), @Type(value = BaseC.class, name = "C")
})
public class Base {
  public enum BaseType { A, B, C }
  ;

  private BaseType baseType;

  public BaseType getBaseType() {
    return baseType;
  }

  public void setBaseType(BaseType baseType) {
    this.baseType = baseType;
  }
}
