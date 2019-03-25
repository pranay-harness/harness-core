package software.wings.graphql.datafetcher;

import static java.util.Arrays.asList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import graphql.schema.DataFetcher;
import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.graphql.utils.GraphQLConstants;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;

import java.util.List;

@Singleton
@Slf4j
public class WorkflowDataFetcher {
  private AuthHandler authHandler;

  private WorkflowService workflowService;

  private WorkflowExecutionService workflowExecutionService;

  @Inject
  public WorkflowDataFetcher(
      WorkflowService workflowService, AuthHandler authHandler, WorkflowExecutionService workflowExecutionService) {
    this.workflowService = workflowService;
    this.authHandler = authHandler;
    this.workflowExecutionService = workflowExecutionService;
  }

  /**
   * TODO authorization code needs to be added here
   *      Need to sync up with @Rama again.
   * @return
   */
  public DataFetcher<PageResponse<Workflow>> getWorkFlows() {
    return environment -> {
      String appId = environment.getArgument(GraphQLConstants.APP_ID);
      String limit = environment.getArgument(GraphQLConstants.PAGE_LIMIT);
      String offSet = environment.getArgument(GraphQLConstants.PAGE_OFFSET);

      PageResponse<Workflow> pageResponse = null;

      if (StringUtils.isNotBlank(appId)) {
        if (StringUtils.isBlank(limit)) {
          limit = GraphQLConstants.PAGE_SIZE_STR;
        }

        if (StringUtils.isBlank(offSet)) {
          offSet = GraphQLConstants.ZERO_OFFSET;
        }

        PageRequest<Workflow> pageRequest = PageRequestBuilder.aPageRequest()
                                                .addFilter(Workflow.APP_ID_KEY, Operator.EQ, appId)
                                                .withLimit(limit)
                                                .withOffset(offSet)
                                                .build();

        pageResponse = workflowService.listWorkflows(pageRequest);
      }

      return pageResponse;
    };
  }

  public DataFetcher<Workflow> getWorkFlow() {
    return environment -> {
      String appId = environment.getArgument(GraphQLConstants.APP_ID);
      String workflowId = environment.getArgument(GraphQLConstants.ENTITY_ID);

      Workflow workflow = null;

      if (isAuthorizedToView(appId, workflowId)) {
        workflow = workflowService.readWorkflow(appId, workflowId);
      } else {
        log.info("User is not authorized to view workflowId = {} of app = {}", workflowId, appId);
      }

      return workflow;
    };
  }

  public DataFetcher<WorkflowExecution> getWorkFlowExecutionStatus() {
    return environment -> {
      String appId = environment.getArgument(GraphQLConstants.APP_ID);
      String workflowId = environment.getArgument(GraphQLConstants.ENTITY_ID);
      String serviceId = environment.getArgument(GraphQLConstants.SERVICE_ID);
      String envId = environment.getArgument(GraphQLConstants.ENV_ID);

      WorkflowExecution workflowExecution = null;

      if (isAuthorizedToView(appId, workflowId)) {
        workflowExecution = workflowExecutionService.fetchLastWorkflowExecution(appId, workflowId, serviceId, envId);
      } else {
        log.info("User is not authorized to view workflowId = {} of app = {}", workflowId, appId);
      }

      return workflowExecution;
    };
  }

  private boolean isAuthorizedToView(String appId, String workflowId) {
    PermissionAttribute permissionAttribute = new PermissionAttribute(PermissionType.WORKFLOW, Action.READ);
    List<PermissionAttribute> permissionAttributeList = asList(permissionAttribute);
    return authHandler.authorize(permissionAttributeList, asList(appId), workflowId);
  }
}
