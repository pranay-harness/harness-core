package software.wings.api;

/**
 * Created by rishi on 12/22/16.
 */
public enum DeploymentType {
  SSH("Secure Shell(SSH)"),
  ECS("EC2 Container Services(ECS)"),
  KUBERNETES("Kubernetes");

  private String displayName;

  DeploymentType(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
