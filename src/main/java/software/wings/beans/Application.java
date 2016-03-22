package software.wings.beans;

import java.util.ArrayList;
import java.util.List;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Reference;

/**
 *  Application bean class.
 *
 *
 * @author Rishi
 *
 */
@Entity(value = "applications", noClassnameStored = true)
public class Application extends Base {
  private String name;
  private String description;
  @Reference(idOnly = true, ignoreMissing = true) private List<Service> services;

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
  public void addService(Service service) {
    if (services == null) {
      services = new ArrayList<>();
    }
    services.add(service);
  }
  public List<Service> getServices() {
    return services;
  }
  public void setServices(List<Service> services) {
    this.services = services;
  }
}
