package software.wings.helpers.ext;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.utils.JsonUtils;

import javax.inject.Inject;

public class JsonUtilsTest {
  @Test
  public void testJson() {
    BaseA baseA = new BaseA();
    String jsona = JsonUtils.asJson(baseA);
    System.out.println(jsona);
    BaseB baseB = new BaseB();
    String jsonb = JsonUtils.asJson(baseB);
    System.out.println(jsonb);

    Base baseA2 = JsonUtils.asObject(jsona, Base.class);
    Base baseB2 = JsonUtils.asObject(jsonb, Base.class);

    System.out.println(baseB2.getBaseType());
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "baseType")
  @JsonSubTypes({
    @JsonSubTypes.Type(value = BaseA.class, name = "A")
    , @JsonSubTypes.Type(value = BaseB.class, name = "B"), @JsonSubTypes.Type(value = BaseC.class, name = "C")
  })
  public static class Base {
    private BaseType baseType;

    public BaseType getBaseType() {
      return baseType;
    }

    public void setBaseType(BaseType baseType) {
      this.baseType = baseType;
    }

    public enum BaseType { A, B, C }
  }

  public static class BaseA extends Base {
    private String name = BaseA.class.getName();

    public BaseA() {
      super();
      setBaseType(BaseType.A);
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  public static class BaseB extends Base {
    private String name = BaseB.class.getName();

    public BaseB() {
      super();
      setBaseType(BaseType.B);
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  public static class BaseC extends Base {
    private String name = BaseC.class.getName();

    public BaseC() {
      super();
      setBaseType(BaseType.C);
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }
}
