package software.wings.graphql.datafetcher.workflow;

import static software.wings.graphql.datafetcher.QueryOperationsEnum.WORKFLOW_EXECUTION;
import static software.wings.graphql.datafetcher.QueryOperationsEnum.WORKFLOW_EXECUTION_LIST;
import static software.wings.graphql.utils.GraphQLConstants.NO_RECORDS_FOUND_FOR_APP_ENV_AND_WORKFLOW;
import static software.wings.graphql.utils.GraphQLConstants.NO_RECORDS_FOUND_FOR_APP_ID_AND_ENTITY;
import static software.wings.graphql.utils.GraphQLConstants.WORKFLOW_EXECUTION_TYPE;
import static software.wings.graphql.utils.GraphQLConstants.WORKFLOW_TYPE;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import graphql.schema.DataFetcher;
import io.harness.beans.PageResponse;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.WorkflowExecution;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.datafetcher.workflow.adapater.WorkflowAdapter;
import software.wings.graphql.schema.type.PagedData;
import software.wings.graphql.schema.type.WorkflowExecutionInfo;
import software.wings.graphql.utils.GraphQLConstants;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.WorkflowExecutionService;

import java.util.Map;
import java.util.stream.Collectors;

@Singleton
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class WorkflowExecutionDataFetcher extends AbstractDataFetcher {
  WorkflowExecutionService workflowExecutionService;
  WorkflowAdapter workflowAdapter;

  @Inject
  public WorkflowExecutionDataFetcher(
      WorkflowExecutionService workflowExecutionService, WorkflowAdapter workflowAdapter, AuthHandler authHandler) {
    super(authHandler);
    this.workflowExecutionService = workflowExecutionService;
    this.workflowAdapter = workflowAdapter;
  }

  @Override
  public Map<String, DataFetcher<?>> getOperationToDataFetcherMap() {
    return ImmutableMap.<String, DataFetcher<?>>builder()
        .put(WORKFLOW_EXECUTION.getOperationName(), getWorkflowExecution())
        .put(WORKFLOW_EXECUTION_LIST.getOperationName(), getWorkflowExecutionList())
        .build();
  }

  private DataFetcher<WorkflowExecutionInfo> getWorkflowExecution() {
    return environment -> {
      WorkflowExecutionInfo workflowExecutionType = WorkflowExecutionInfo.builder().build();

      String appId = environment.getArgument(GraphQLConstants.APP_ID);
      if (StringUtils.isBlank(appId)) {
        addInvalidInputInfo(workflowExecutionType, GraphQLConstants.APP_ID);
        return workflowExecutionType;
      }

      String workflowId = environment.getArgument(GraphQLConstants.WORKFLOW_ID);
      if (StringUtils.isBlank(workflowId)) {
        addInvalidInputInfo(workflowExecutionType, GraphQLConstants.WORKFLOW_ID);
        return workflowExecutionType;
      }

      if (!isAuthorizedToView(appId, workflowId)) {
        throwNotAuthorizedException(WORKFLOW_TYPE, workflowId, appId);
      }

      String envId = environment.getArgument(GraphQLConstants.ENV_ID);
      String serviceId = environment.getArgument(GraphQLConstants.SERVICE_ID);
      WorkflowExecution workflowExecution =
          workflowExecutionService.fetchLastWorkflowExecution(appId, workflowId, serviceId, envId);
      if (workflowExecution != null) {
        workflowExecutionType = workflowAdapter.getWorkflowExecution(workflowExecution);
      } else {
        addNoRecordFoundInfo(
            workflowExecutionType, NO_RECORDS_FOUND_FOR_APP_ID_AND_ENTITY, WORKFLOW_EXECUTION_TYPE, workflowId, appId);
      }

      return workflowExecutionType;
    };
  }

  private DataFetcher<PagedData<WorkflowExecutionInfo>> getWorkflowExecutionList() {
    return environment -> {
      PagedData<WorkflowExecutionInfo> pagedData = PagedData.<WorkflowExecutionInfo>builder().build();

      String appId = environment.getArgument(GraphQLConstants.APP_ID);
      if (StringUtils.isBlank(appId)) {
        addInvalidInputInfo(pagedData, GraphQLConstants.APP_ID);
        return pagedData;
      }

      String workflowId = environment.getArgument(GraphQLConstants.WORKFLOW_ID);
      if (StringUtils.isBlank(workflowId)) {
        addInvalidInputInfo(pagedData, GraphQLConstants.WORKFLOW_ID);
        return pagedData;
      }

      String envId = environment.getArgument(GraphQLConstants.ENV_ID);
      if (StringUtils.isBlank(envId)) {
        addInvalidInputInfo(pagedData, GraphQLConstants.ENV_ID);
        return pagedData;
      }

      int limit = getPageLimit(environment);
      int offset = getPageOffset(environment);
      PageResponse<WorkflowExecution> pageResponse =
          workflowExecutionService.fetchWorkflowExecutionList(appId, workflowId, envId, offset, limit);

      pagedData.setTotal(pageResponse.getTotal());
      pagedData.setLimit(limit);
      pagedData.setOffset(offset);

      if (pageResponse.getResponse().isEmpty()) {
        addNoRecordFoundInfo(
            pagedData, NO_RECORDS_FOUND_FOR_APP_ENV_AND_WORKFLOW, WORKFLOW_EXECUTION_TYPE, appId, envId, workflowId);
      } else {
        pagedData.setData(pageResponse.getResponse()
                              .stream()
                              .map(we -> workflowAdapter.getWorkflowExecution(we))
                              .collect(Collectors.toList()));
      }

      return pagedData;
    };
  }

  private boolean isAuthorizedToView(String appId, String workflowId) {
    PermissionAttribute permissionAttribute = new PermissionAttribute(PermissionType.WORKFLOW, Action.READ);
    return isAuthorizedToView(appId, permissionAttribute, workflowId);
  }
}
