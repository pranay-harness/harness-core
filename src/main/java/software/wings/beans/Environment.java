package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Reference;

import java.util.List;

/**
 *  Environment bean class.
 *
 *
 * @author Rishi
 *
 */

@Entity(value = "environments", noClassnameStored = true)
public class Environment extends Base {
  @Indexed private String applicationId;
  private String name;
  private String description;

  @Reference(idOnly = true, ignoreMissing = true) private List<Infra> infras;

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
  public String getApplicationId() {
    return applicationId;
  }
  public void setApplicationId(String applicationId) {
    this.applicationId = applicationId;
  }
}
