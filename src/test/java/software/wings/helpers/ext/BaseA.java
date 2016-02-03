package software.wings.helpers.ext;

public class BaseA extends Base {
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
