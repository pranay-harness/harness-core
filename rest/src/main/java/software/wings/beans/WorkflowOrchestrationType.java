package software.wings.beans;

/**
 * Created by rishi on 12/21/16.
 */
public enum WorkflowOrchestrationType {
  CANARY("Canary Deployment Workflow"),
  BLUE_GREEN("Blue/Green Deployment Workflow");

  private final String display;
  WorkflowOrchestrationType(String display) {
    this.display = display;
  }
}
