package software.wings.graphql.datafetcher.workflow;

import static software.wings.graphql.utils.GraphQLConstants.NO_RECORDS_FOUND_FOR_APP_ENV_AND_WORKFLOW;
import static software.wings.graphql.utils.GraphQLConstants.WORKFLOW_EXECUTION_TYPE;

import com.google.inject.Inject;

import graphql.schema.DataFetchingEnvironment;
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
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.WorkflowExecutionService;

import java.util.stream.Collectors;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class WorkflowExecutionListDataFetcher extends AbstractDataFetcher<PagedData<WorkflowExecutionInfo>> {
  WorkflowExecutionService workflowExecutionService;
  WorkflowAdapter workflowAdapter;

  @Inject
  public WorkflowExecutionListDataFetcher(
      WorkflowExecutionService workflowExecutionService, WorkflowAdapter workflowAdapter, AuthHandler authHandler) {
    super(authHandler);
    this.workflowExecutionService = workflowExecutionService;
    this.workflowAdapter = workflowAdapter;
  }

  @Override
  public PagedData<WorkflowExecutionInfo> get(DataFetchingEnvironment dataFetchingEnvironment) throws Exception {
    PagedData<WorkflowExecutionInfo> pagedData = PagedData.<WorkflowExecutionInfo>builder().build();

    String appId = (String) getArgumentValue(dataFetchingEnvironment, GraphQLConstants.APP_ID);
    if (StringUtils.isBlank(appId)) {
      addInvalidInputInfo(pagedData, GraphQLConstants.APP_ID);
      return pagedData;
    }

    String workflowId = (String) getArgumentValue(dataFetchingEnvironment, GraphQLConstants.WORKFLOW_ID);
    if (StringUtils.isBlank(workflowId)) {
      addInvalidInputInfo(pagedData, GraphQLConstants.WORKFLOW_ID);
      return pagedData;
    }

    String envId = (String) getArgumentValue(dataFetchingEnvironment, GraphQLConstants.ENV_ID);
    if (StringUtils.isBlank(envId)) {
      addInvalidInputInfo(pagedData, GraphQLConstants.ENV_ID);
      return pagedData;
    }

    int limit = getPageLimit(dataFetchingEnvironment);
    int offset = getPageOffset(dataFetchingEnvironment);
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
  }
}
