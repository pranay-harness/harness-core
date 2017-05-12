package software.wings.service.impl.appdynamics;

/**
 * Created by rsingh on 4/17/17.
 */
public class AppdynamicsApplication {
  private String name;
  private String description;
  private int id;

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

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    AppdynamicsApplication that = (AppdynamicsApplication) o;

    return id == that.id;
  }

  @Override
  public int hashCode() {
    return id;
  }

  @Override
  public String toString() {
    return "AppdynamicsApplication{"
        + "name='" + name + '\'' + ", description='" + description + '\'' + ", id=" + id + '}';
  }
}
