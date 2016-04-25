package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;

/**
 * Environment bean class.
 *
 * @author Rishi
 */
@Entity(value = "environments", noClassnameStored = true)
public class Environment extends Base {
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
