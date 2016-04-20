package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;

import java.util.List;

/**
 * Created by anubhaw on 4/4/16.
 */
@Entity(value = "serviceTemplates", noClassnameStored = true)
public class ServiceTemplate extends Base {
  private String serviceID;
  private String name;
  private String description;
  private List<Tag> tags;
  private List<Host> hosts;
  private String envID;

  public String getServiceID() {
    return serviceID;
  }

  public void setServiceID(String serviceID) {
    this.serviceID = serviceID;
  }

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

  public List<Tag> getTags() {
    return tags;
  }

  public void setTags(List<Tag> tags) {
    this.tags = tags;
  }

  public List<Host> getHosts() {
    return hosts;
  }

  public void setHosts(List<Host> hosts) {
    this.hosts = hosts;
  }

  public String getEnvID() {
    return envID;
  }

  public void setEnvID(String envID) {
    this.envID = envID;
  }
}
