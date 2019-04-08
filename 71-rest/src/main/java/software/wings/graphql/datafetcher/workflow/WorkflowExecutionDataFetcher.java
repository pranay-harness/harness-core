package software.wings.graphql.datafetcher.workflow;

import static software.wings.graphql.utils.GraphQLConstants.NO_RECORDS_FOUND_FOR_APP_ID_AND_ENTITY;
import static software.wings.graphql.utils.GraphQLConstants.WORKFLOW_EXECUTION_TYPE;
import static software.wings.graphql.utils.GraphQLConstants.WORKFLOW_TYPE;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import graphql.schema.DataFetchingEnvironment;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.WorkflowExecution;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.datafetcher.workflow.adapater.WorkflowAdapter;
import software.wings.graphql.schema.type.WorkflowExecutionInfo;
import software.wings.graphql.utils.GraphQLConstants;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.WorkflowExecutionService;

@Singleton
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class WorkflowExecutionDataFetcher extends AbstractDataFetcher<WorkflowExecutionInfo> {
  WorkflowExecutionService workflowExecutionService;
  WorkflowAdapter workflowAdapter;

  @Inject
  public WorkflowExecutionDataFetcher(
      WorkflowExecutionService workflowExecutionService, WorkflowAdapter workflowAdapter, AuthHandler authHandler) {
    super(authHandler);
    this.workflowExecutionService = workflowExecutionService;
    this.workflowAdapter = workflowAdapter;
  }

  private boolean isAuthorizedToView(String appId, String workflowId) {
    PermissionAttribute permissionAttribute = new PermissionAttribute(PermissionType.WORKFLOW, Action.READ);
    return isAuthorizedToView(appId, permissionAttribute, workflowId);
  }

  @Override
  public Object get(DataFetchingEnvironment dataFetchingEnvironment) throws Exception {
    WorkflowExecutionInfo workflowExecutionType = WorkflowExecutionInfo.builder().build();

    String appId = dataFetchingEnvironment.getArgument(GraphQLConstants.APP_ID);
    if (StringUtils.isBlank(appId)) {
      addInvalidInputInfo(workflowExecutionType, GraphQLConstants.APP_ID);
      return workflowExecutionType;
    }

    String workflowId = dataFetchingEnvironment.getArgument(GraphQLConstants.WORKFLOW_ID);
    if (StringUtils.isBlank(workflowId)) {
      addInvalidInputInfo(workflowExecutionType, GraphQLConstants.WORKFLOW_ID);
      return workflowExecutionType;
    }

    if (!isAuthorizedToView(appId, workflowId)) {
      throwNotAuthorizedException(WORKFLOW_TYPE, workflowId, appId);
    }

    String envId = dataFetchingEnvironment.getArgument(GraphQLConstants.ENV_ID);
    String serviceId = dataFetchingEnvironment.getArgument(GraphQLConstants.SERVICE_ID);
    WorkflowExecution workflowExecution =
        workflowExecutionService.fetchLastWorkflowExecution(appId, workflowId, serviceId, envId);
    if (workflowExecution != null) {
      workflowExecutionType = workflowAdapter.getWorkflowExecution(workflowExecution);
    } else {
      addNoRecordFoundInfo(
          workflowExecutionType, NO_RECORDS_FOUND_FOR_APP_ID_AND_ENTITY, WORKFLOW_EXECUTION_TYPE, workflowId, appId);
    }

    return workflowExecutionType;
  }
}
