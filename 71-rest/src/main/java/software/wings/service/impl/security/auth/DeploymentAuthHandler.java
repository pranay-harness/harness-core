package software.wings.service.impl.security.auth;

import static io.harness.beans.WorkflowType.ORCHESTRATION;
import static io.harness.beans.WorkflowType.PIPELINE;
import static io.harness.validation.Validator.notNullCheck;
import static java.util.Arrays.asList;
import static software.wings.security.PermissionAttribute.Action.EXECUTE_PIPELINE;
import static software.wings.security.PermissionAttribute.Action.EXECUTE_WORKFLOW;
import static software.wings.security.PermissionAttribute.PermissionType.DEPLOYMENT;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.Pipeline;
import software.wings.beans.User;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.security.PermissionAttribute;
import software.wings.security.UserRequestContext;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * Created by ujjawal on 05/24/20.
 */
@Singleton
@Slf4j
public class DeploymentAuthHandler {
  @Inject private AuthService authService;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private WorkflowService workflowService;
  @Inject private PipelineService pipelineService;

  private void authorize(List<PermissionAttribute> requiredPermissionAttributes, List<String> appIds, String entityId) {
    User user = UserThreadLocal.get();
    if (user != null && user.getUserRequestContext() != null) {
      UserRequestContext userRequestContext = user.getUserRequestContext();
      if (userRequestContext != null) {
        authService.authorize(userRequestContext.getAccountId(), appIds, entityId, user, requiredPermissionAttributes);
      }
    }
  }

  public void authorize(String appId, String workflowExecutionId) {
    List<PermissionAttribute> permissionAttributeList = new ArrayList<>();
    WorkflowExecution workflowExecution =
        workflowExecutionService.getExecutionDetailsWithoutGraph(appId, workflowExecutionId);

    if (workflowExecution.getPipelineSummary() != null) {
      String pipelineId = workflowExecution.getPipelineSummary().getPipelineId();
      notNullCheck("Pipeline id is null for execution " + workflowExecutionId, pipelineId);
      permissionAttributeList.add(new PermissionAttribute(DEPLOYMENT, EXECUTE_PIPELINE));
      authorize(permissionAttributeList, asList(appId), pipelineId);
    } else {
      String workflowId = workflowExecution.getWorkflowId();
      notNullCheck("Workflow id is null for execution " + workflowExecutionId, workflowId);
      permissionAttributeList.add(new PermissionAttribute(DEPLOYMENT, EXECUTE_WORKFLOW));
      authorize(permissionAttributeList, asList(appId), workflowId);
    }
    checkPermissionToDeployEnv(appId, workflowExecution);
  }

  public void authorize(String appId, WorkflowExecution workflowExecution) {
    if (workflowExecution != null && workflowExecution.getWorkflowId() != null) {
      String workflowId = workflowExecution.getWorkflowId();
      List<PermissionAttribute> permissionAttributeList = getPermissionAttributeList(workflowExecution);
      authorize(permissionAttributeList, Collections.singletonList(appId), workflowId);
    }
    checkPermissionToDeployEnv(appId, workflowExecution);
  }

  private List<PermissionAttribute> getPermissionAttributeList(WorkflowExecution workflowExecution) {
    if (workflowExecution.getWorkflowType() == PIPELINE) {
      return Collections.singletonList(new PermissionAttribute(DEPLOYMENT, EXECUTE_PIPELINE));
    } else {
      return Collections.singletonList(new PermissionAttribute(DEPLOYMENT, EXECUTE_WORKFLOW));
    }
  }

  public void authorizeWithWorkflowExecutionId(String appId, String workflowExecutionId) {
    WorkflowExecution workflowExecution = workflowExecutionService.getWorkflowExecution(appId, workflowExecutionId);
    authorize(appId, workflowExecution);
  }

  public void authorizePipelineExecution(String appId, String entityId) {
    List<PermissionAttribute> permissionAttributeList = new ArrayList<>();
    permissionAttributeList.add(new PermissionAttribute(DEPLOYMENT, EXECUTE_PIPELINE));
    authorize(permissionAttributeList, Collections.singletonList(appId), entityId);
  }

  public void authorizeWorkflowExecution(String appId, String entityId) {
    List<PermissionAttribute> permissionAttributeList = new ArrayList<>();
    permissionAttributeList.add(new PermissionAttribute(DEPLOYMENT, EXECUTE_WORKFLOW));
    authorize(permissionAttributeList, Collections.singletonList(appId), entityId);
  }

  public void authorizeWorkflowOrPipelineForExecution(@NotNull String appId, @NotNull String workflowOrPipelineId) {
    Workflow workflow = workflowService.getWorkflow(appId, workflowOrPipelineId);
    if (workflow != null) {
      authorizeWorkflowExecution(appId, workflowOrPipelineId);
    }
    Pipeline pipeline = pipelineService.getPipeline(appId, workflowOrPipelineId);
    if (pipeline != null) {
      authorizePipelineExecution(appId, workflowOrPipelineId);
    }
    if (workflow == null && pipeline == null) {
      logger.error(
          "Provided workflowOrPipelineId for Auth is neither a workflow nor a pipeline, with appId {} and entityId {}",
          appId, workflowOrPipelineId);
      throw new InvalidRequestException(
          "Provided workflowOrPipelineId for authorization is neither a workflow nor a pipeline");
    }
  }

  private void checkPermissionToDeployEnv(String appId, WorkflowExecution workflowExecution) {
    if (workflowExecution != null && StringUtils.isNotEmpty(workflowExecution.getEnvId())) {
      if (ORCHESTRATION == workflowExecution.getWorkflowType()) {
        authService.checkIfUserAllowedToDeployWorkflowToEnv(appId, workflowExecution.getEnvId());
      } else {
        authService.checkIfUserAllowedToDeployPipelineToEnv(appId, workflowExecution.getEnvId());
      }
    }
  }
}