package software.wings.helpers.ext;

public class BaseC extends Base {
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
