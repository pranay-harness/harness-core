package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;

/**
 * Created by anubhaw on 4/1/16.
 */
@Entity(value = "infras", noClassnameStored = true)
public class Infra extends Base {
  private InfraType infraType;
  private String envID;

  public InfraType getInfraType() {
    return infraType;
  }

  public void setInfraType(InfraType infraType) {
    this.infraType = infraType;
  }

  public String getEnvID() {
    return envID;
  }

  public void setEnvID(String envID) {
    this.envID = envID;
  }

  public static enum InfraType { STATIC, AWS, AZURE, CONTAINER }
}
