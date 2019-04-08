package software.wings.graphql.datafetcher.workflow;

import static software.wings.graphql.utils.GraphQLConstants.APP_ID;
import static software.wings.graphql.utils.GraphQLConstants.ENV_ID;
import static software.wings.graphql.utils.GraphQLConstants.NO_RECORDS_FOUND_FOR_APP_ID_AND_ENTITY;
import static software.wings.graphql.utils.GraphQLConstants.WORKFLOW_TYPE;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import graphql.GraphQLContext;
import graphql.schema.DataFetchingEnvironment;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.Workflow;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.datafetcher.workflow.adapater.WorkflowAdapter;
import software.wings.graphql.schema.type.WorkflowInfo;
import software.wings.graphql.utils.GraphQLConstants;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.WorkflowService;

@Singleton
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WorkflowDataFetcher extends AbstractDataFetcher<WorkflowInfo> {
  WorkflowService workflowService;

  WorkflowAdapter workflowAdapter;

  @Inject
  public WorkflowDataFetcher(
      WorkflowService workflowService, WorkflowAdapter workflowAdapter, AuthHandler authHandler) {
    super(authHandler);
    this.workflowService = workflowService;
    this.workflowAdapter = workflowAdapter;
  }

  private boolean isAuthorizedToView(String appId, String workflowId) {
    PermissionAttribute permissionAttribute = new PermissionAttribute(PermissionType.WORKFLOW, Action.READ);
    return isAuthorizedToView(appId, permissionAttribute, workflowId);
  }

  @Override
  public Object get(DataFetchingEnvironment dataFetchingEnvironment) throws Exception {
    WorkflowInfo workflowInfo = WorkflowInfo.builder().build();
    String appId = dataFetchingEnvironment.getArgument(GraphQLConstants.APP_ID);
    String workflowId = dataFetchingEnvironment.getArgument(GraphQLConstants.ENTITY_ID);
    // Pre-checks
    if (StringUtils.isBlank(appId)) {
      addInvalidInputInfo(workflowInfo, GraphQLConstants.APP_ID);
      return workflowInfo;
    }

    if (StringUtils.isBlank(workflowId)) {
      addInvalidInputInfo(workflowInfo, GraphQLConstants.ENTITY_ID);
      return workflowInfo;
    }

    if (!this.isAuthorizedToView(appId, workflowId)) {
      throwNotAuthorizedException(WORKFLOW_TYPE, workflowId, appId);
    }

    Workflow workflow = workflowService.readWorkflow(appId, workflowId);
    if (workflow != null) {
      workflowInfo = workflowAdapter.getWorkflow(workflow);
      GraphQLContext.Builder builder = dataFetchingEnvironment.getLocalContext();
      builder.of(APP_ID, workflow.getAppId());
      builder.of(ENV_ID, workflow.getEnvId());
    } else {
      addNoRecordFoundInfo(workflowInfo, NO_RECORDS_FOUND_FOR_APP_ID_AND_ENTITY, WORKFLOW_TYPE, workflowId, appId);
    }
    return workflowInfo;
  }
}
