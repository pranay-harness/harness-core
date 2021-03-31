package io.harness.pms.approval.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.exception.WingsException.USER_SRE;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResourceClient;
import io.harness.data.structure.CollectionUtils;
import io.harness.delegate.TaskDetails;
import io.harness.delegate.TaskMode;
import io.harness.delegate.TaskSelector;
import io.harness.delegate.TaskSetupAbstractions;
import io.harness.delegate.TaskType;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.jira.JiraConnectorDTO;
import io.harness.delegate.task.jira.JiraTaskNGParameters;
import io.harness.delegate.task.jira.JiraTaskNGResponse;
import io.harness.engine.pms.tasks.NgDelegate2TaskExecutor;
import io.harness.exception.GeneralException;
import io.harness.exception.HarnessJiraException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.jira.JiraActionNG;
import io.harness.jira.JiraIssueNG;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccessWithEncryptionConsumer;
import io.harness.pms.contracts.execution.tasks.DelegateTaskRequest;
import io.harness.pms.contracts.execution.tasks.TaskCategory;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.remote.client.NGRestUtils;
import io.harness.remote.client.RestClientUtils;
import io.harness.repositories.ApprovalInstanceRepository;
import io.harness.secretmanagerclient.remote.SecretManagerClient;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.approval.step.beans.ApprovalStatus;
import io.harness.steps.approval.step.entities.ApprovalInstance.ApprovalInstanceKeys;
import io.harness.steps.approval.step.jira.JiraApprovalHelperService;
import io.harness.steps.approval.step.jira.beans.CriteriaSpecDTO;
import io.harness.steps.approval.step.jira.beans.JiraApprovalResponseData;
import io.harness.steps.approval.step.jira.entities.JiraApprovalInstance;
import io.harness.steps.approval.step.jira.entities.JiraApprovalInstance.JiraApprovalInstanceKeys;
import io.harness.steps.approval.step.jira.evaluation.ConditionEvaluationHelper;
import io.harness.tasks.BinaryResponseData;
import io.harness.tasks.ResponseData;
import io.harness.utils.IdentifierRefHelper;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.Duration;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.mapping.Mapper;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(CDC)
@Slf4j
public class JiraApprovalHelperServiceImpl implements JiraApprovalHelperService {
  private final NgDelegate2TaskExecutor ngDelegate2TaskExecutor;
  private final ConnectorResourceClient connectorResourceClient;
  private final KryoSerializer kryoSerializer;
  private final SecretManagerClient secretManagerClient;
  private final WaitNotifyEngine waitNotifyEngine;
  private final ApprovalInstanceRepository approvalInstanceRepository;

  @Inject
  public JiraApprovalHelperServiceImpl(NgDelegate2TaskExecutor ngDelegate2TaskExecutor,
      ConnectorResourceClient connectorResourceClient, KryoSerializer kryoSerializer,
      SecretManagerClient secretManagerClient, WaitNotifyEngine waitNotifyEngine,
      ApprovalInstanceRepository approvalInstanceRepository) {
    this.ngDelegate2TaskExecutor = ngDelegate2TaskExecutor;
    this.connectorResourceClient = connectorResourceClient;
    this.kryoSerializer = kryoSerializer;
    this.secretManagerClient = secretManagerClient;
    this.waitNotifyEngine = waitNotifyEngine;
    this.approvalInstanceRepository = approvalInstanceRepository;
  }

  @Override
  public void handlePollingEvent(JiraApprovalInstance entity) {
    log.info(
        "Polling Approval status for Jira Approval Instance {} approval type {}", entity.getId(), entity.getType());
    try {
      String instanceId = entity.getId();
      String accountIdentifier = entity.getAccountIdentifier();
      String orgIdentifier = entity.getOrgIdentifier();
      String projectIdentifier = entity.getProjectIdentifier();
      String issueKey = entity.getIssueKey();
      String connectorRef = entity.getConnectorRef();

      validateField(instanceId, ApprovalInstanceKeys.id);
      validateField(accountIdentifier, ApprovalInstanceKeys.accountIdentifier);
      validateField(orgIdentifier, ApprovalInstanceKeys.orgIdentifier);
      validateField(projectIdentifier, ApprovalInstanceKeys.projectIdentifier);
      validateField(issueKey, JiraApprovalInstanceKeys.issueKey);
      validateField(connectorRef, JiraApprovalInstanceKeys.connectorRef);

      JiraTaskNGParameters jiraTaskNGParameters =
          prepareJiraTaskParameters(accountIdentifier, orgIdentifier, projectIdentifier, issueKey, connectorRef);
      JiraTaskNGResponse jiraTaskNGResponse = fetchJiraTaskResponse(accountIdentifier, jiraTaskNGParameters);
      if (isNull(jiraTaskNGResponse.getIssue())) {
        throw new HarnessJiraException("Missing Issue in JiraTaskNGResponse", USER_SRE);
      }
      checkApprovalAndRejectionCriteria(jiraTaskNGResponse.getIssue(), entity);
    } catch (Exception ex) {
      log.warn(
          "Error occurred while processing JiraApproval. Continuing to poll next minute. Id: {}", entity.getId(), ex);
    }
  }

  private void checkApprovalAndRejectionCriteria(JiraIssueNG issue, JiraApprovalInstance jiraApprovalInstance) {
    try {
      if (isNull(jiraApprovalInstance.getApprovalCriteria())
          || isNull(jiraApprovalInstance.getApprovalCriteria().getCriteriaSpecDTO())) {
        throw new InvalidRequestException("Approval criteria can't be missing");
      }
      CriteriaSpecDTO approvalCriteriaSpec = jiraApprovalInstance.getApprovalCriteria().getCriteriaSpecDTO();
      boolean approvalEvaluationResult = ConditionEvaluationHelper.evaluateCondition(issue, approvalCriteriaSpec);
      if (approvalEvaluationResult) {
        log.info("Approval Criteria for JiraApprovalInstance {} has been met", jiraApprovalInstance.getId());
        updateJiraApprovalInstance(jiraApprovalInstance, ApprovalStatus.APPROVED);
        waitNotifyEngine.doneWith(jiraApprovalInstance.getId(),
            JiraApprovalResponseData.builder().instanceId(jiraApprovalInstance.getId()).build());
        return;
      }

      if (!isNull(jiraApprovalInstance.getApprovalCriteria())
          && !isNull(jiraApprovalInstance.getApprovalCriteria().getCriteriaSpecDTO())) {
        CriteriaSpecDTO rejectionCriteriaSpec = jiraApprovalInstance.getApprovalCriteria().getCriteriaSpecDTO();
        boolean rejectionEvaluationResult = ConditionEvaluationHelper.evaluateCondition(issue, rejectionCriteriaSpec);
        if (rejectionEvaluationResult) {
          log.info("Rejection Criteria for JiraApprovalInstance {} has been met", jiraApprovalInstance.getId());
          updateJiraApprovalInstance(jiraApprovalInstance, ApprovalStatus.REJECTED);
          waitNotifyEngine.doneWith(jiraApprovalInstance.getId(),
              JiraApprovalResponseData.builder().instanceId(jiraApprovalInstance.getId()).build());
        }
      }
    } catch (Exception e) {
      throw new HarnessJiraException("Error while evaluating Approval/Rejection criteria", e, USER_SRE);
    }
  }

  private JiraTaskNGParameters prepareJiraTaskParameters(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String issueId, String connectorRef) throws IOException {
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

  private JiraTaskNGResponse fetchJiraTaskResponse(
      String accountIdentifier, JiraTaskNGParameters jiraTaskNGParameters) {
    TaskRequest jiraTaskRequest = prepareJiraTaskRequest(accountIdentifier, jiraTaskNGParameters);
    ResponseData response = ngDelegate2TaskExecutor.executeTask(new HashMap<>(), jiraTaskRequest);
    Object responseObject = kryoSerializer.asInflatedObject(((BinaryResponseData) response).getData());
    if (responseObject instanceof ErrorNotifyResponseData) {
      ErrorNotifyResponseData errorResponse = (ErrorNotifyResponseData) responseObject;
      throw new GeneralException(format("Failed to fetch jira response. Error: %s", errorResponse.getErrorMessage()));
    }
    return (JiraTaskNGResponse) responseObject;
  }

  private TaskRequest prepareJiraTaskRequest(String accountIdentifier, JiraTaskNGParameters jiraTaskNGParameters) {
    DelegateTaskRequest.Builder requestBuilder =
        DelegateTaskRequest.newBuilder()
            .setAccountId(accountIdentifier)
            .setDetails(
                TaskDetails.newBuilder()
                    .setKryoParameters(ByteString.copyFrom(kryoSerializer.asDeflatedBytes(jiraTaskNGParameters) == null
                            ? new byte[] {}
                            : kryoSerializer.asDeflatedBytes(jiraTaskNGParameters)))
                    .setExecutionTimeout(Duration.newBuilder().setSeconds(20).build())
                    .setMode(TaskMode.SYNC)
                    .setParked(false)
                    .setType(TaskType.newBuilder().setType(software.wings.beans.TaskType.JIRA_TASK_NG.name()).build())
                    .build())
            .addAllSelectors(jiraTaskNGParameters.getDelegateSelectors()
                                 .stream()
                                 .map(s -> TaskSelector.newBuilder().setSelector(s).build())
                                 .collect(Collectors.toList()))
            .addAllLogKeys(CollectionUtils.emptyIfNull(null))
            .setSetupAbstractions(TaskSetupAbstractions.newBuilder().build())
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
            String.format("Connector not found for identifier : [%s]", connectorIdentifierRef), WingsException.USER);
      }
      ConnectorInfoDTO connectorInfoDTO = connectorDTO.get().getConnectorInfo();
      ConnectorConfigDTO connectorConfigDTO = connectorInfoDTO.getConnectorConfig();
      if (connectorConfigDTO instanceof JiraConnectorDTO) {
        return (JiraConnectorDTO) connectorConfigDTO;
      }
      throw new HarnessJiraException(
          format("Connector of other then Jira type was found : [%s] ", connectorIdentifierRef), USER_SRE);
    } catch (Exception e) {
      throw new HarnessJiraException(
          format("Error while getting connector information : [%s]", connectorIdentifierRef), USER_SRE);
    }
  }

  private void updateJiraApprovalInstance(JiraApprovalInstance jiraApprovalInstance, ApprovalStatus status) {
    jiraApprovalInstance.setStatus(status);
    approvalInstanceRepository.updateFirst(new Query(Criteria.where(Mapper.ID_KEY).is(jiraApprovalInstance.getId())),
        new Update().set(ApprovalInstanceKeys.status, status));
  }

  private void validateField(String name, String value) {
    if (isBlank(value)) {
      throw new InvalidRequestException(format("Field %s can't be empty", name));
    }
  }
}
