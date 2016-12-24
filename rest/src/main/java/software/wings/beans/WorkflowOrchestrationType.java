package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Created by rishi on 12/21/16.
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum WorkflowOrchestrationType {
  CANARY("Canary Deployment Workflow"),
  BLUE_GREEN("Blue/Green Deployment Workflow");

  private final String display;
  WorkflowOrchestrationType(String display) {
    this.display = display;
  }

  public String getDisplay() {
    return display;
  }
}
