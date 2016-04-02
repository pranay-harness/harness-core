package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anubhaw on 4/1/16.
 */

@Entity(value = "infras", noClassnameStored = true)
public class Infra extends Base {
  public static enum InfraType { STATIC, AWS, AZURE, CONTAINER }

  private InfraType infraType;

  @Reference(idOnly = true, ignoreMissing = true) private List<Host> hosts;

  public InfraType getInfraType() {
    return infraType;
  }

  public void setInfraType(InfraType infraType) {
    this.infraType = infraType;
  }

  public List<Host> getHosts() {
    return hosts;
  }

  public void setHosts(List<Host> hosts) {
    this.hosts = hosts;
  }

  public void addHost(Host host) {
    if (this.hosts == null) {
      this.hosts = new ArrayList<>();
    }
    this.hosts.add(host);
  }
}
