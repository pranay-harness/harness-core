package software.wings.helpers.ext;

public class BaseB extends Base {
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
