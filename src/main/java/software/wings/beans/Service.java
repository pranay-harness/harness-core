package software.wings.beans;

/**
 *  Component bean class.
 *
 *
 * @author Rishi
 *
 */

public class Service extends Base {
  private String name;

  private String description;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
