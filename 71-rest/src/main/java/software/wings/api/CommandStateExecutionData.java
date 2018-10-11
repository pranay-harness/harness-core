package software.wings.api;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.govern.Switch.unhandled;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.CountsByStatuses;
import software.wings.beans.command.CodeDeployParams;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.CommandUnitDetails;
import software.wings.beans.command.ContainerSetupParams;
import software.wings.beans.command.KubernetesSetupParams;
import software.wings.service.intfc.ActivityService;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StepExecutionSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by peeyushaggarwal on 6/17/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class CommandStateExecutionData extends StateExecutionData {
  @Transient private static final Logger logger = LoggerFactory.getLogger(CommandStateExecutionData.class);
  private String appId;
  private String delegateTaskId;
  private String hostName;
  private String publicDns;
  private String hostId;
  private String commandName;
  private String serviceName;
  private String serviceId;
  private String templateName;
  private String templateId;
  private String activityId;
  private String artifactId;
  private String artifactName;
  private CountsByStatuses countsByStatuses;
  private List<ContainerServiceData> newInstanceData;
  private List<ContainerServiceData> oldInstanceData;
  private boolean downsize;
  private String clusterName;

  private List<InstanceStatusSummary> newInstanceStatusSummaries = new ArrayList<>();

  private CodeDeployParams codeDeployParams;
  private CodeDeployParams oldCodeDeployParams;
  private String codeDeployDeploymentId;
  private ContainerSetupParams containerSetupParams;

  @Transient @Inject private transient ActivityService activityService;

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> data = super.getExecutionSummary();
    if (isNotEmpty(appId) && isNotEmpty(activityId) && activityService != null) {
      if (countsByStatuses == null) {
        try {
          List<CommandUnitDetails> commandUnits = activityService.getCommandUnits(appId, activityId);
          countsByStatuses = new CountsByStatuses();
          commandUnits.forEach(commandUnit -> {
            final CommandExecutionStatus commandExecutionStatus = commandUnit.getCommandExecutionStatus();
            switch (commandExecutionStatus) {
              case SUCCESS:
                countsByStatuses.setSuccess(countsByStatuses.getSuccess() + 1);
                break;
              case FAILURE:
                countsByStatuses.setFailed(countsByStatuses.getFailed() + 1);
                break;
              case RUNNING:
                countsByStatuses.setInprogress(countsByStatuses.getInprogress() + 1);
                break;
              case QUEUED:
                countsByStatuses.setQueued(countsByStatuses.getQueued() + 1);
                break;
              default:
                unhandled(commandExecutionStatus);
            }
          });
        } catch (Exception e) {
          logger.error("Failed to retrieve command units for appId {} and activityId {} ", appId, activityId, e);
        }
      }
      if (countsByStatuses != null) {
        data.put("total",
            ExecutionDataValue.builder()
                .displayName("Total")
                .value(countsByStatuses.getFailed() + countsByStatuses.getInprogress() + countsByStatuses.getSuccess()
                    + countsByStatuses.getQueued())
                .build());
        data.put("breakdown", ExecutionDataValue.builder().displayName("breakdown").value(countsByStatuses).build());
      }
    }
    putNotNull(data, "hostName", ExecutionDataValue.builder().displayName("Host").value(hostName).build());
    putNotNull(data, "templateName", ExecutionDataValue.builder().displayName("Config").value(templateName).build());
    putNotNull(data, "commandName", ExecutionDataValue.builder().displayName("Command").value(commandName).build());

    return data;
  }

  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "hostName", ExecutionDataValue.builder().displayName("Host").value(hostName).build());
    putNotNull(executionDetails, "templateName",
        ExecutionDataValue.builder().displayName("Config").value(templateName).build());
    putNotNull(executionDetails, "commandName",
        ExecutionDataValue.builder().displayName("Command").value(commandName).build());
    putNotNull(executionDetails, "activityId", ExecutionDataValue.builder().displayName("").value(activityId).build());
    return executionDetails;
  }

  @Override
  public StepExecutionSummary getStepExecutionSummary() {
    CommandStepExecutionSummary commandStepExecutionSummary = new CommandStepExecutionSummary();
    populateStepExecutionSummary(commandStepExecutionSummary);
    if (isNotEmpty(newInstanceData)) {
      commandStepExecutionSummary.setNewInstanceData(newInstanceData);
    }
    if (isNotEmpty(oldInstanceData)) {
      commandStepExecutionSummary.setOldInstanceData(oldInstanceData);
    }
    if (containerSetupParams instanceof KubernetesSetupParams) {
      KubernetesSetupParams kubernetesSetupParams = (KubernetesSetupParams) containerSetupParams;
      commandStepExecutionSummary.setControllerNamePrefix(kubernetesSetupParams.getControllerNamePrefix());
    }
    commandStepExecutionSummary.setClusterName(clusterName);
    commandStepExecutionSummary.setServiceId(serviceId);
    commandStepExecutionSummary.setCodeDeployParams(codeDeployParams);
    commandStepExecutionSummary.setOldCodeDeployParams(oldCodeDeployParams);
    commandStepExecutionSummary.setCodeDeployDeploymentId(codeDeployDeploymentId);
    return commandStepExecutionSummary;
  }

  public static final class Builder {
    private String stateName;
    private String stateType;
    private Long startTs;
    private Long endTs;
    private ExecutionStatus status;
    private String errorMsg;
    private Integer waitInterval;
    private ContextElement element;
    private Map<String, Object> stateParams;
    private String appId;
    private String delegateTaskId;
    private String hostName;
    private String publicDns;
    private String hostId;
    private String commandName;
    private String serviceName;
    private String serviceId;
    private String templateName;
    private String templateId;
    private String activityId;
    private String artifactId;
    private String artifactName;
    private CountsByStatuses countsByStatuses;
    private List<ContainerServiceData> newInstanceData = new ArrayList<>();
    private List<ContainerServiceData> oldInstanceData = new ArrayList<>();
    private boolean downsize;
    private String clusterName;
    private List<InstanceStatusSummary> newInstanceStatusSummaries = new ArrayList<>();
    private CodeDeployParams codeDeployParams;
    private CodeDeployParams oldCodeDeployParams;
    private String codeDeployDeploymentId;
    private ContainerSetupParams containerSetupParams;
    private transient ActivityService activityService;
    private Map<String, Object> templateVariable;

    private Builder() {}

    public static Builder aCommandStateExecutionData() {
      return new Builder();
    }

    public Builder withStateName(String stateName) {
      this.stateName = stateName;
      return this;
    }

    public Builder withStateType(String stateType) {
      this.stateType = stateType;
      return this;
    }

    public Builder withStartTs(Long startTs) {
      this.startTs = startTs;
      return this;
    }

    public Builder withEndTs(Long endTs) {
      this.endTs = endTs;
      return this;
    }

    public Builder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    public Builder withErrorMsg(String errorMsg) {
      this.errorMsg = errorMsg;
      return this;
    }

    public Builder withWaitInterval(Integer waitInterval) {
      this.waitInterval = waitInterval;
      return this;
    }

    public Builder withElement(ContextElement element) {
      this.element = element;
      return this;
    }

    public Builder withStateParams(Map<String, Object> stateParams) {
      this.stateParams = stateParams;
      return this;
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withDelegateTaskId(String delegateTaskId) {
      this.delegateTaskId = delegateTaskId;
      return this;
    }

    public Builder withHostName(String hostName) {
      this.hostName = hostName;
      return this;
    }

    public Builder withPublicDns(String publicDns) {
      this.publicDns = publicDns;
      return this;
    }

    public Builder withHostId(String hostId) {
      this.hostId = hostId;
      return this;
    }

    public Builder withCommandName(String commandName) {
      this.commandName = commandName;
      return this;
    }

    public Builder withServiceName(String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    public Builder withServiceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    public Builder withTemplateName(String templateName) {
      this.templateName = templateName;
      return this;
    }

    public Builder withTemplateId(String templateId) {
      this.templateId = templateId;
      return this;
    }

    public Builder withActivityId(String activityId) {
      this.activityId = activityId;
      return this;
    }

    public Builder withArtifactId(String artifactId) {
      this.artifactId = artifactId;
      return this;
    }

    public Builder withArtifactName(String artifactName) {
      this.artifactName = artifactName;
      return this;
    }

    public Builder withCountsByStatuses(CountsByStatuses countsByStatuses) {
      this.countsByStatuses = countsByStatuses;
      return this;
    }

    public Builder withNewInstanceData(List<ContainerServiceData> newInstanceData) {
      this.newInstanceData = newInstanceData;
      return this;
    }

    public Builder withOldInstanceData(List<ContainerServiceData> oldInstanceData) {
      this.oldInstanceData = oldInstanceData;
      return this;
    }

    public Builder withDownsize(boolean downsize) {
      this.downsize = downsize;
      return this;
    }

    public Builder withClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    public Builder withNewInstanceStatusSummaries(List<InstanceStatusSummary> newInstanceStatusSummaries) {
      this.newInstanceStatusSummaries = newInstanceStatusSummaries;
      return this;
    }

    public Builder withCodeDeployParams(CodeDeployParams codeDeployParams) {
      this.codeDeployParams = codeDeployParams;
      return this;
    }

    public Builder withOldCodeDeployParams(CodeDeployParams oldCodeDeployParams) {
      this.oldCodeDeployParams = oldCodeDeployParams;
      return this;
    }

    public Builder withCodeDeploymentId(String codeDeployDeploymentId) {
      this.codeDeployDeploymentId = codeDeployDeploymentId;
      return this;
    }

    public Builder withContainerSetupParams(ContainerSetupParams containerSetupParams) {
      this.containerSetupParams = containerSetupParams;
      return this;
    }

    public Builder withActivityService(ActivityService activityService) {
      this.activityService = activityService;
      return this;
    }

    public Builder withTemplateVariable(Map<String, Object> var) {
      this.templateVariable = var;
      return this;
    }

    public Builder but() {
      return aCommandStateExecutionData()
          .withStateName(stateName)
          .withStateType(stateType)
          .withStartTs(startTs)
          .withEndTs(endTs)
          .withStatus(status)
          .withErrorMsg(errorMsg)
          .withWaitInterval(waitInterval)
          .withElement(element)
          .withStateParams(stateParams)
          .withAppId(appId)
          .withDelegateTaskId(delegateTaskId)
          .withHostName(hostName)
          .withPublicDns(publicDns)
          .withHostId(hostId)
          .withCommandName(commandName)
          .withServiceName(serviceName)
          .withServiceId(serviceId)
          .withTemplateName(templateName)
          .withTemplateId(templateId)
          .withActivityId(activityId)
          .withArtifactId(artifactId)
          .withArtifactName(artifactName)
          .withCountsByStatuses(countsByStatuses)
          .withNewInstanceData(newInstanceData)
          .withOldInstanceData(oldInstanceData)
          .withDownsize(downsize)
          .withClusterName(clusterName)
          .withNewInstanceStatusSummaries(newInstanceStatusSummaries)
          .withCodeDeployParams(codeDeployParams)
          .withOldCodeDeployParams(oldCodeDeployParams)
          .withCodeDeploymentId(codeDeployDeploymentId)
          .withContainerSetupParams(containerSetupParams)
          .withActivityService(activityService);
    }

    public CommandStateExecutionData build() {
      CommandStateExecutionData commandStateExecutionData = new CommandStateExecutionData();
      commandStateExecutionData.setStateName(stateName);
      commandStateExecutionData.setStateType(stateType);
      commandStateExecutionData.setStartTs(startTs);
      commandStateExecutionData.setEndTs(endTs);
      commandStateExecutionData.setStatus(status);
      commandStateExecutionData.setErrorMsg(errorMsg);
      commandStateExecutionData.setWaitInterval(waitInterval);
      commandStateExecutionData.setElement(element);
      commandStateExecutionData.setStateParams(stateParams);
      commandStateExecutionData.setAppId(appId);
      commandStateExecutionData.setDelegateTaskId(delegateTaskId);
      commandStateExecutionData.setHostName(hostName);
      commandStateExecutionData.setPublicDns(publicDns);
      commandStateExecutionData.setHostId(hostId);
      commandStateExecutionData.setCommandName(commandName);
      commandStateExecutionData.setServiceName(serviceName);
      commandStateExecutionData.setServiceId(serviceId);
      commandStateExecutionData.setTemplateName(templateName);
      commandStateExecutionData.setTemplateId(templateId);
      commandStateExecutionData.setActivityId(activityId);
      commandStateExecutionData.setArtifactId(artifactId);
      commandStateExecutionData.setArtifactName(artifactName);
      commandStateExecutionData.setCountsByStatuses(countsByStatuses);
      commandStateExecutionData.setNewInstanceData(newInstanceData);
      commandStateExecutionData.setOldInstanceData(oldInstanceData);
      commandStateExecutionData.setDownsize(downsize);
      commandStateExecutionData.setClusterName(clusterName);
      commandStateExecutionData.setNewInstanceStatusSummaries(newInstanceStatusSummaries);
      commandStateExecutionData.setCodeDeployParams(codeDeployParams);
      commandStateExecutionData.setOldCodeDeployParams(oldCodeDeployParams);
      commandStateExecutionData.setCodeDeployDeploymentId(codeDeployDeploymentId);
      commandStateExecutionData.setContainerSetupParams(containerSetupParams);
      commandStateExecutionData.setActivityService(activityService);
      commandStateExecutionData.setTemplateVariable(templateVariable);
      return commandStateExecutionData;
    }
  }
}
