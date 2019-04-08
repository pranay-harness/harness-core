package software.wings.graphql.datafetcher.workflow;

import static software.wings.graphql.utils.GraphQLConstants.NO_RECORDS_FOUND_FOR_APP_ID;
import static software.wings.graphql.utils.GraphQLConstants.WORKFLOW_TYPE;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import graphql.schema.DataFetchingEnvironment;
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
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.WorkflowService;

import java.util.stream.Collectors;

@Singleton
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WorkflowListDataFetcher extends AbstractDataFetcher<PagedData<WorkflowInfo>> {
  WorkflowService workflowService;

  WorkflowAdapter workflowAdapter;

  @Inject
  public WorkflowListDataFetcher(
      WorkflowService workflowService, WorkflowAdapter workflowAdapter, AuthHandler authHandler) {
    super(authHandler);
    this.workflowService = workflowService;
    this.workflowAdapter = workflowAdapter;
  }

  @Override
  public Object get(DataFetchingEnvironment dataFetchingEnvironment) throws Exception {
    PagedData<WorkflowInfo> pagedData = PagedData.<WorkflowInfo>builder().build();
    String appId = dataFetchingEnvironment.getArgument(GraphQLConstants.APP_ID);

    if (StringUtils.isBlank(appId)) {
      addInvalidInputInfo(pagedData, GraphQLConstants.APP_ID);
      return pagedData;
    }

    int limit = getPageLimit(dataFetchingEnvironment);
    int offset = getPageOffset(dataFetchingEnvironment);

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
  }
}
