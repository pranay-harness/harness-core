package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.mongodb.morphia.annotations.Embedded;

/**
 * Created by anubhaw on 2/6/17.
 */
@Embedded
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "deploymentType")
public abstract class ContainerTask {
  private String deploymentType;

  public ContainerTask(String deploymentType) {
    this.deploymentType = deploymentType;
  }

  public String getDeploymentType() {
    return deploymentType;
  }
}
