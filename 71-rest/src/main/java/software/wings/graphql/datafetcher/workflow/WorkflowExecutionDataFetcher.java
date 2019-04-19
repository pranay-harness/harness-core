package software.wings.graphql.datafetcher.workflow;

import static software.wings.graphql.utils.GraphQLConstants.NO_RECORDS_FOUND_FOR_APP_ID_AND_ENTITY;
import static software.wings.graphql.utils.GraphQLConstants.WORKFLOW_EXECUTION_TYPE;
import static software.wings.graphql.utils.GraphQLConstants.WORKFLOW_TYPE;

import com.google.inject.Inject;

import graphql.schema.DataFetchingEnvironment;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.WorkflowExecution;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.schema.type.QLWorkflowExecution;
import software.wings.graphql.utils.GraphQLConstants;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.WorkflowExecutionService;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class WorkflowExecutionDataFetcher extends AbstractDataFetcher<QLWorkflowExecution> {
  WorkflowExecutionService workflowExecutionService;

  @Inject
  public WorkflowExecutionDataFetcher(WorkflowExecutionService workflowExecutionService, AuthHandler authHandler) {
    super(authHandler);
    this.workflowExecutionService = workflowExecutionService;
  }

  private boolean isAuthorizedToView(String appId, String workflowId) {
    PermissionAttribute permissionAttribute = new PermissionAttribute(PermissionType.WORKFLOW, Action.READ);
    return isAuthorizedToView(appId, permissionAttribute, workflowId);
  }

  @Override
  public QLWorkflowExecution fetch(DataFetchingEnvironment dataFetchingEnvironment) {
    QLWorkflowExecution workflowExecutionType = QLWorkflowExecution.builder().build();

    String appId = (String) getArgumentValue(dataFetchingEnvironment, GraphQLConstants.APP_ID_ARG);
    if (StringUtils.isBlank(appId)) {
      addInvalidInputInfo(workflowExecutionType, GraphQLConstants.APP_ID_ARG);
      return workflowExecutionType;
    }

    String workflowId = (String) getArgumentValue(dataFetchingEnvironment, GraphQLConstants.WORKFLOW_ID);
    if (StringUtils.isBlank(workflowId)) {
      addInvalidInputInfo(workflowExecutionType, GraphQLConstants.WORKFLOW_ID);
      return workflowExecutionType;
    }

    if (!isAuthorizedToView(appId, workflowId)) {
      throw notAuthorizedException(WORKFLOW_TYPE, workflowId, appId);
    }

    String envId = dataFetchingEnvironment.getArgument(GraphQLConstants.ENV_ID_ARG);
    String serviceId = dataFetchingEnvironment.getArgument(GraphQLConstants.SERVICE_ID_ARG);
    WorkflowExecution workflowExecution =
        workflowExecutionService.fetchLastWorkflowExecution(appId, workflowId, serviceId, envId);
    if (workflowExecution != null) {
      workflowExecutionType = WorkflowController.getWorkflowExecution(workflowExecution);
    } else {
      addNoRecordFoundInfo(
          workflowExecutionType, NO_RECORDS_FOUND_FOR_APP_ID_AND_ENTITY, WORKFLOW_EXECUTION_TYPE, workflowId, appId);
    }

    return workflowExecutionType;
  }
}
