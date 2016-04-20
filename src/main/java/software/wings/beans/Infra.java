package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;

/**
 * Created by anubhaw on 4/1/16.
 */
@Entity(value = "infras", noClassnameStored = true)
public class Infra extends Base {
  private InfraType infraType;
  private String envId;

  public InfraType getInfraType() {
    return infraType;
  }

  public void setInfraType(InfraType infraType) {
    this.infraType = infraType;
  }

  public String getEnvId() {
    return envId;
  }

  public void setEnvId(String envId) {
    this.envId = envId;
  }

  public static enum InfraType { STATIC, AWS, AZURE, CONTAINER }
}
