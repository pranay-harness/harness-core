package software.wings.beans;

import java.util.ArrayList;
import java.util.List;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;

/**
 *  Application bean class.
 *
 *
 * @author Rishi
 *
 */
@Entity(value = "applications", noClassnameStored = true)
public class Application extends Base {
  @Indexed private String name;

  private String description;

  private List<Service> services = new ArrayList<>();

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
  public List<Service> getServices() {
    return services;
  }
  public void setServices(List<Service> services) {
    this.services = services;
  }
}
