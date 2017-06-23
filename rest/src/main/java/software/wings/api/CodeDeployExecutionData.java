package software.wings.api;

import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;

import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionData;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Map;

/**
 * Created by brett on 6/23/17
 */
public class CodeDeployExecutionData extends StateExecutionData implements NotifyResponseData {
  private String revision;
  private String commandName;

  public String getRevision() {
    return revision;
  }

  public void setRevision(String revision) {
    this.revision = revision;
  }

  public String getCommandName() {
    return commandName;
  }

  public void setCommandName(String commandName) {
    this.commandName = commandName;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
    putNotNull(
        executionDetails, "revision", anExecutionDataValue().withValue(revision).withDisplayName("Revision").build());
    putNotNull(executionDetails, "commandName",
        anExecutionDataValue().withValue(commandName).withDisplayName("Command Name").build());
    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(
        executionDetails, "revision", anExecutionDataValue().withValue(revision).withDisplayName("Revision").build());
    putNotNull(executionDetails, "commandName",
        anExecutionDataValue().withValue(commandName).withDisplayName("Command Name").build());
    return executionDetails;
  }

  public static final class CodeDeployExecutionDataBuilder {
    private String revision;
    private String commandName;
    private String stateName;
    private Long startTs;
    private Long endTs;
    private ExecutionStatus status;
    private String errorMsg;

    private CodeDeployExecutionDataBuilder() {}

    public static CodeDeployExecutionDataBuilder aCodeDeployExecutionData() {
      return new CodeDeployExecutionDataBuilder();
    }

    public CodeDeployExecutionDataBuilder withRevision(String revision) {
      this.revision = revision;
      return this;
    }

    public CodeDeployExecutionDataBuilder withStateName(String stateName) {
      this.stateName = stateName;
      return this;
    }

    public CodeDeployExecutionDataBuilder withStartTs(Long startTs) {
      this.startTs = startTs;
      return this;
    }

    public CodeDeployExecutionDataBuilder withEndTs(Long endTs) {
      this.endTs = endTs;
      return this;
    }

    public CodeDeployExecutionDataBuilder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    public CodeDeployExecutionDataBuilder withErrorMsg(String errorMsg) {
      this.errorMsg = errorMsg;
      return this;
    }

    public CodeDeployExecutionDataBuilder withCommandName(String commandName) {
      this.commandName = commandName;
      return this;
    }

    public CodeDeployExecutionDataBuilder but() {
      return aCodeDeployExecutionData()
          .withRevision(revision)
          .withStateName(stateName)
          .withStartTs(startTs)
          .withEndTs(endTs)
          .withStatus(status)
          .withErrorMsg(errorMsg)
          .withCommandName(commandName);
    }

    public CodeDeployExecutionData build() {
      CodeDeployExecutionData kubernetesReplicationControllerExecutionData = new CodeDeployExecutionData();
      kubernetesReplicationControllerExecutionData.setRevision(revision);
      kubernetesReplicationControllerExecutionData.setStateName(stateName);
      kubernetesReplicationControllerExecutionData.setStartTs(startTs);
      kubernetesReplicationControllerExecutionData.setEndTs(endTs);
      kubernetesReplicationControllerExecutionData.setStatus(status);
      kubernetesReplicationControllerExecutionData.setErrorMsg(errorMsg);
      kubernetesReplicationControllerExecutionData.setCommandName(commandName);
      return kubernetesReplicationControllerExecutionData;
    }
  }
}
