package software.wings.graphql.datafetcher.workflow;

import static software.wings.graphql.datafetcher.QueryOperationsEnum.WORKFLOW;
import static software.wings.graphql.datafetcher.QueryOperationsEnum.WORKFLOW_LIST;
import static software.wings.graphql.utils.GraphQLConstants.NO_RECORDS_FOUND_FOR_APP_ID;
import static software.wings.graphql.utils.GraphQLConstants.NO_RECORDS_FOUND_FOR_APP_ID_AND_ENTITY;
import static software.wings.graphql.utils.GraphQLConstants.WORKFLOW_TYPE;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import graphql.schema.DataFetcher;
import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.Workflow;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.datafetcher.workflow.adapater.WorkflowAdapter;
import software.wings.graphql.schema.type.PagedData;
import software.wings.graphql.schema.type.WorkflowInfo;
import software.wings.graphql.utils.GraphQLConstants;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.WorkflowService;

import java.util.Map;
import java.util.stream.Collectors;

@Singleton
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WorkflowDataFetcher extends AbstractDataFetcher {
  WorkflowService workflowService;

  WorkflowAdapter workflowAdapter;

  @Inject
  public WorkflowDataFetcher(
      WorkflowService workflowService, WorkflowAdapter workflowAdapter, AuthHandler authHandler) {
    super(authHandler);
    this.workflowService = workflowService;
    this.workflowAdapter = workflowAdapter;
  }

  @Override
  public Map<String, DataFetcher<?>> getOperationToDataFetcherMap() {
    return ImmutableMap.<String, DataFetcher<?>>builder()
        .put(WORKFLOW.getOperationName(), getWorkflow())
        .put(WORKFLOW_LIST.getOperationName(), getWorkflows())
        .build();
  }

  /**
   * TODO authorization code needs to be added here
   * Need to sync up with @Rama again.
   *
   * @return
   */
  private DataFetcher<PagedData<WorkflowInfo>> getWorkflows() {
    return environment -> {
      PagedData<WorkflowInfo> pagedData = PagedData.<WorkflowInfo>builder().build();
      String appId = environment.getArgument(GraphQLConstants.APP_ID);

      if (StringUtils.isBlank(appId)) {
        addInvalidInputInfo(pagedData, GraphQLConstants.APP_ID);
        return pagedData;
      }

      int limit = getPageLimit(environment);
      int offset = getPageOffset(environment);

      PageRequest<Workflow> pageRequest = PageRequestBuilder.aPageRequest()
                                              .addFilter(Workflow.APP_ID_KEY, Operator.EQ, appId)
                                              .withLimit(String.valueOf(limit))
                                              .withOffset(String.valueOf(offset))
                                              .build();

      PageResponse<Workflow> pageResponse = workflowService.listWorkflows(pageRequest);

      pagedData.setTotal(pageResponse.getTotal());
      pagedData.setLimit(limit);
      pagedData.setOffset(offset);

      if (pageResponse.getResponse().isEmpty()) {
        addNoRecordFoundInfo(pagedData, NO_RECORDS_FOUND_FOR_APP_ID, WORKFLOW_TYPE, appId);
      } else {
        pagedData.setData(
            pageResponse.getResponse().stream().map(w -> workflowAdapter.getWorkflow(w)).collect(Collectors.toList()));
      }
      return pagedData;
    };
  }

  private DataFetcher<WorkflowInfo> getWorkflow() {
    return environment -> {
      WorkflowInfo workflowInfo = WorkflowInfo.builder().build();
      String appId = environment.getArgument(GraphQLConstants.APP_ID);
      String workflowId = environment.getArgument(GraphQLConstants.ENTITY_ID);
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
      } else {
        addNoRecordFoundInfo(workflowInfo, NO_RECORDS_FOUND_FOR_APP_ID_AND_ENTITY, WORKFLOW_TYPE, workflowId, appId);
      }
      return workflowInfo;
    };
  }

  private boolean isAuthorizedToView(String appId, String workflowId) {
    PermissionAttribute permissionAttribute = new PermissionAttribute(PermissionType.WORKFLOW, Action.READ);
    return isAuthorizedToView(appId, permissionAttribute, workflowId);
  }
}
