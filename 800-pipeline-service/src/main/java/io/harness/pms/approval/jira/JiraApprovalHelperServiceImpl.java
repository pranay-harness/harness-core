package io.harness.pms.approval.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.OrchestrationPublisherName;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResourceClient;
import io.harness.delegate.TaskDetails;
import io.harness.delegate.TaskMode;
import io.harness.delegate.TaskSelector;
import io.harness.delegate.TaskSetupAbstractions;
import io.harness.delegate.TaskType;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.jira.JiraConnectorDTO;
import io.harness.delegate.task.jira.JiraTaskNGParameters;
import io.harness.engine.pms.tasks.NgDelegate2TaskExecutor;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.HarnessJiraException;
import io.harness.exception.InvalidRequestException;
import io.harness.jira.JiraActionNG;
import io.harness.logging.AutoLogContext;
import io.harness.logstreaming.LogStreamingStepClientFactory;
import io.harness.logstreaming.NGLogCallback;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccessWithEncryptionConsumer;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.tasks.DelegateTaskRequest;
import io.harness.pms.contracts.execution.tasks.TaskCategory;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.remote.client.NGRestUtils;
import io.harness.remote.client.RestClientUtils;
import io.harness.secretmanagerclient.remote.SecretManagerClient;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepUtils;
import io.harness.steps.approval.step.entities.ApprovalInstance.ApprovalInstanceKeys;
import io.harness.steps.approval.step.jira.JiraApprovalHelperService;
import io.harness.steps.approval.step.jira.entities.JiraApprovalInstance;
import io.harness.steps.approval.step.jira.entities.JiraApprovalInstance.JiraApprovalInstanceKeys;
import io.harness.utils.IdentifierRefHelper;
import io.harness.waiter.NotifyCallback;
import io.harness.waiter.WaitNotifyEngine;

import software.wings.beans.LogColor;
import software.wings.beans.LogHelper;
import software.wings.beans.LogWeight;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;

@OwnedBy(CDC)
@Slf4j
public class JiraApprovalHelperServiceImpl implements JiraApprovalHelperService {
  private final NgDelegate2TaskExecutor ngDelegate2TaskExecutor;
  private final ConnectorResourceClient connectorResourceClient;
  private final KryoSerializer kryoSerializer;
  private final SecretManagerClient secretManagerClient;
  private final WaitNotifyEngine waitNotifyEngine;
  private final LogStreamingStepClientFactory logStreamingStepClientFactory;
  private final String publisherName;

  @Inject
  public JiraApprovalHelperServiceImpl(NgDelegate2TaskExecutor ngDelegate2TaskExecutor,
      ConnectorResourceClient connectorResourceClient, KryoSerializer kryoSerializer,
      SecretManagerClient secretManagerClient, WaitNotifyEngine waitNotifyEngine,
      LogStreamingStepClientFactory logStreamingStepClientFactory,
      @Named(OrchestrationPublisherName.PUBLISHER_NAME) String publisherName) {
    this.ngDelegate2TaskExecutor = ngDelegate2TaskExecutor;
    this.connectorResourceClient = connectorResourceClient;
    this.kryoSerializer = kryoSerializer;
    this.secretManagerClient = secretManagerClient;
    this.waitNotifyEngine = waitNotifyEngine;
    this.logStreamingStepClientFactory = logStreamingStepClientFactory;
    this.publisherName = publisherName;
  }

  @Override
  public void handlePollingEvent(JiraApprovalInstance instance) {
    try (AutoLogContext ignore = instance.autoLogContext()) {
      handlePollingEventInternal(instance);
    }
  }

  private void handlePollingEventInternal(JiraApprovalInstance instance) {
    Ambiance ambiance = instance.getAmbiance();
    NGLogCallback logCallback = new NGLogCallback(
        logStreamingStepClientFactory, ambiance, null, instance.getVersion() == null || instance.getVersion() == 0);

    try {
      log.info("Polling jira approval instance");
      logCallback.saveExecutionLog("-----");
      logCallback.saveExecutionLog(
          LogHelper.color("Fetching jira issue to check approval/rejection criteria", LogColor.White, LogWeight.Bold));

      String instanceId = instance.getId();
      String accountIdentifier = AmbianceUtils.getAccountId(ambiance);
      String orgIdentifier = AmbianceUtils.getOrgIdentifier(ambiance);
      String projectIdentifier = AmbianceUtils.getProjectIdentifier(ambiance);
      String issueKey = instance.getIssueKey();
      String connectorRef = instance.getConnectorRef();

      validateField(instanceId, ApprovalInstanceKeys.id);
      validateField(accountIdentifier, "accountIdentifier");
      validateField(orgIdentifier, "orgIdentifier");
      validateField(projectIdentifier, "projectIdentifier");
      validateField(issueKey, JiraApprovalInstanceKeys.issueKey);
      validateField(connectorRef, JiraApprovalInstanceKeys.connectorRef);

      JiraTaskNGParameters jiraTaskNGParameters =
          prepareJiraTaskParameters(accountIdentifier, orgIdentifier, projectIdentifier, issueKey, connectorRef);
      logCallback.saveExecutionLog(
          String.format("Jira url: %s", jiraTaskNGParameters.getJiraConnectorDTO().getJiraUrl()));

      String taskId = queueTask(ambiance, instanceId, jiraTaskNGParameters);
      logCallback.saveExecutionLog(String.format("Jira task: %s", taskId));
    } catch (Exception ex) {
      logCallback.saveExecutionLog(
          String.format("Error creating task for fetching jira issue: %s", ExceptionUtils.getMessage(ex)));
      log.warn("Error creating task for fetching jira issue while polling", ex);
    }
  }

  private JiraTaskNGParameters prepareJiraTaskParameters(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String issueId, String connectorRef) {
    JiraConnectorDTO jiraConnectorDTO =
        getJiraConnector(accountIdentifier, orgIdentifier, projectIdentifier, connectorRef);
    BaseNGAccess baseNGAccess = BaseNGAccess.builder()
                                    .accountIdentifier(accountIdentifier)
                                    .orgIdentifier(orgIdentifier)
                                    .projectIdentifier(projectIdentifier)
                                    .build();

    NGAccessWithEncryptionConsumer ngAccessWithEncryptionConsumer =
        NGAccessWithEncryptionConsumer.builder().ngAccess(baseNGAccess).decryptableEntity(jiraConnectorDTO).build();
    List<EncryptedDataDetail> encryptionDataDetails =
        RestClientUtils.getResponse(secretManagerClient.getEncryptionDetails(ngAccessWithEncryptionConsumer));

    return JiraTaskNGParameters.builder()
        .action(JiraActionNG.GET_ISSUE)
        .encryptionDetails(encryptionDataDetails)
        .jiraConnectorDTO(jiraConnectorDTO)
        .issueKey(issueId)
        .build();
  }

  private String queueTask(Ambiance ambiance, String approvalInstanceId, JiraTaskNGParameters jiraTaskNGParameters) {
    TaskRequest jiraTaskRequest = prepareJiraTaskRequest(ambiance, jiraTaskNGParameters);
    String taskId =
        ngDelegate2TaskExecutor.queueTask(ambiance.getSetupAbstractionsMap(), jiraTaskRequest, Duration.ofSeconds(0));
    NotifyCallback callback = JiraApprovalCallback.builder().approvalInstanceId(approvalInstanceId).build();
    waitNotifyEngine.waitForAllOn(publisherName, callback, taskId);
    return taskId;
  }

  private TaskRequest prepareJiraTaskRequest(Ambiance ambiance, JiraTaskNGParameters jiraTaskNGParameters) {
    final String accountId = AmbianceUtils.getAccountId(ambiance);
    LinkedHashMap<String, String> logAbstractionMap = StepUtils.generateLogAbstractions(ambiance);
    DelegateTaskRequest.Builder requestBuilder =
        DelegateTaskRequest.newBuilder()
            .setAccountId(accountId)
            .setDetails(
                TaskDetails.newBuilder()
                    .setKryoParameters(ByteString.copyFrom(kryoSerializer.asDeflatedBytes(jiraTaskNGParameters) == null
                            ? new byte[] {}
                            : kryoSerializer.asDeflatedBytes(jiraTaskNGParameters)))
                    .setExecutionTimeout(com.google.protobuf.Duration.newBuilder().setSeconds(20).build())
                    .setMode(TaskMode.ASYNC)
                    .setParked(false)
                    .setType(TaskType.newBuilder().setType(software.wings.beans.TaskType.JIRA_TASK_NG.name()).build())
                    .build())
            .addAllSelectors(jiraTaskNGParameters.getDelegateSelectors()
                                 .stream()
                                 .map(s -> TaskSelector.newBuilder().setSelector(s).build())
                                 .collect(Collectors.toList()))
            .addAllLogKeys(StepUtils.generateLogKeys(logAbstractionMap, Collections.emptyList()))
            .setSetupAbstractions(TaskSetupAbstractions.newBuilder()
                                      .putAllValues(MapUtils.emptyIfNull(ambiance.getSetupAbstractionsMap()))
                                      .build())
            .setSelectionTrackingLogEnabled(true);

    return TaskRequest.newBuilder()
        .setDelegateTaskRequest(requestBuilder.build())
        .setTaskCategory(TaskCategory.DELEGATE_TASK_V2)
        .build();
  }

  @Override
  public JiraConnectorDTO getJiraConnector(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifierRef) {
    try {
      IdentifierRef connectorRef = IdentifierRefHelper.getIdentifierRef(
          connectorIdentifierRef, accountIdentifier, orgIdentifier, projectIdentifier);
      Optional<ConnectorDTO> connectorDTO = NGRestUtils.getResponse(
          connectorResourceClient.get(connectorRef.getIdentifier(), connectorRef.getAccountIdentifier(),
              connectorRef.getOrgIdentifier(), connectorRef.getProjectIdentifier()));

      if (!connectorDTO.isPresent()) {
        throw new InvalidRequestException(
            String.format("Connector not found for identifier : [%s]", connectorIdentifierRef));
      }
      ConnectorInfoDTO connectorInfoDTO = connectorDTO.get().getConnectorInfo();
      ConnectorConfigDTO connectorConfigDTO = connectorInfoDTO.getConnectorConfig();
      if (connectorConfigDTO instanceof JiraConnectorDTO) {
        return (JiraConnectorDTO) connectorConfigDTO;
      }
      throw new HarnessJiraException(
          format("Connector of other then Jira type was found : [%s] ", connectorIdentifierRef));
    } catch (Exception e) {
      throw new HarnessJiraException(
          format("Error while getting connector information : [%s]", connectorIdentifierRef));
    }
  }

  private void validateField(String name, String value) {
    if (isBlank(value)) {
      throw new InvalidRequestException(format("Field %s can't be empty", name));
    }
  }
}
