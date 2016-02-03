package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Reference;

@Entity(value = "hostInstanceMapping", noClassnameStored = true)
public class HostInstanceMapping extends Base {
  @Indexed private String applicationId;

  @Reference(idOnly = true) private Host host;

  private String instanceUuid;

  public Host getHost() {
    return host;
  }
  public void setHost(Host host) {
    this.host = host;
  }
  public String getInstanceUuid() {
    return instanceUuid;
  }
  public void setInstanceUuid(String instanceUuid) {
    this.instanceUuid = instanceUuid;
  }
  public String getApplicationId() {
    return applicationId;
  }
  public void setApplicationId(String applicationId) {
    this.applicationId = applicationId;
  }
}
