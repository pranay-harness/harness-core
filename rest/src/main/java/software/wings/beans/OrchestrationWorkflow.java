package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;

import java.util.List;
import java.util.Set;

/**
 * Created by rishi on 3/28/17.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "orchestrationWorkflowType", include = As.PROPERTY)
@JsonSubTypes({
  @JsonSubTypes.Type(value = CanaryOrchestrationWorkflow.class, name = "CANARY")
  , @JsonSubTypes.Type(value = CustomOrchestrationWorkflow.class, name = "CUSTOM")
})
public abstract class OrchestrationWorkflow {
  private OrchestrationWorkflowType orchestrationWorkflowType;

  private boolean valid;

  private String validationMessage;

  public OrchestrationWorkflowType getOrchestrationWorkflowType() {
    return orchestrationWorkflowType;
  }

  public void setOrchestrationWorkflowType(OrchestrationWorkflowType orchestrationWorkflowType) {
    this.orchestrationWorkflowType = orchestrationWorkflowType;
  }

  public boolean isValid() {
    return valid;
  }

  public void setValid(boolean valid) {
    this.valid = valid;
  }

  public String getValidationMessage() {
    return validationMessage;
  }

  public void setValidationMessage(String validationMessage) {
    this.validationMessage = validationMessage;
  }

  public abstract List<String> getServiceIds();

  public abstract void onSave();

  public abstract void onLoad();

  public abstract Set<EntityType> getRequiredEntityTypes();

  public abstract void setRequiredEntityTypes(Set<EntityType> requiredEntityTypes);

  public abstract boolean validate();
}
