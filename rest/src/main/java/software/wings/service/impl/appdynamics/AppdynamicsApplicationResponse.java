package software.wings.service.impl.appdynamics;

/**
 * Created by rsingh on 4/17/17.
 */
public class AppdynamicsApplicationResponse {
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
}
