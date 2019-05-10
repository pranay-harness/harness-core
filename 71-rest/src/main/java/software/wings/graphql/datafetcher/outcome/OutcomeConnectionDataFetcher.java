package software.wings.graphql.datafetcher.outcome;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;

import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.WorkflowExecution;
import software.wings.graphql.datafetcher.AbstractConnectionDataFetcher;
import software.wings.graphql.datafetcher.environment.EnvironmentController;
import software.wings.graphql.datafetcher.execution.ExecutionController;
import software.wings.graphql.datafetcher.execution.WorkflowExecutionController;
import software.wings.graphql.datafetcher.service.ServiceController;
import software.wings.graphql.schema.query.QLOutcomesQueryParameters;
import software.wings.graphql.schema.type.QLDeploymentOutcome;
import software.wings.graphql.schema.type.QLDeploymentOutcome.QLDeploymentOutcomeBuilder;
import software.wings.graphql.schema.type.QLEnvironment;
import software.wings.graphql.schema.type.QLEnvironment.QLEnvironmentBuilder;
import software.wings.graphql.schema.type.QLOutcomeConnection;
import software.wings.graphql.schema.type.QLOutcomeConnection.QLOutcomeConnectionBuilder;
import software.wings.graphql.schema.type.QLPageInfo;
import software.wings.graphql.schema.type.QLPageInfo.QLPageInfoBuilder;
import software.wings.graphql.schema.type.QLService;
import software.wings.graphql.schema.type.QLService.QLServiceBuilder;
import software.wings.graphql.schema.type.QLWorkflowExecution;
import software.wings.graphql.schema.type.QLWorkflowExecution.QLWorkflowExecutionBuilder;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

@Slf4j
public class OutcomeConnectionDataFetcher
    extends AbstractConnectionDataFetcher<QLOutcomeConnection, QLOutcomesQueryParameters> {
  @Inject private HPersistence persistence;
  @Inject private WorkflowExecutionController workflowExecutionController;
  @Inject private ExecutionController executionController;

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  public QLOutcomeConnection fetchConnection(QLOutcomesQueryParameters qlQuery) {
    WorkflowExecution execution = persistence.get(WorkflowExecution.class, qlQuery.getExecutionId());

    QLOutcomeConnectionBuilder connectionBuilder = QLOutcomeConnection.builder();

    QLPageInfoBuilder pageInfoBuilder = QLPageInfo.builder().hasMore(false).offset(0).limit(0).total(0);

    if (isNotEmpty(execution.getServiceExecutionSummaries())) {
      pageInfoBuilder.total(execution.getServiceExecutionSummaries().size())
          .limit(execution.getServiceExecutionSummaries().size());

      final Environment environment = persistence.get(Environment.class, execution.getEnvId());
      QLEnvironmentBuilder environmentBuilder = QLEnvironment.builder();
      EnvironmentController.populateEnvironment(environment, environmentBuilder);

      for (ElementExecutionSummary summary : execution.getServiceExecutionSummaries()) {
        QLWorkflowExecutionBuilder workflowExecutionBuilder = QLWorkflowExecution.builder();
        workflowExecutionController.populateWorkflowExecution(execution, workflowExecutionBuilder);

        QLDeploymentOutcomeBuilder deployment =
            QLDeploymentOutcome.builder().execution(workflowExecutionBuilder.build());

        if (qlQuery.isServiceRequested()) {
          // TODO: RBAC this
          final Service service = persistence.get(Service.class, summary.getContextElement().getUuid());
          QLServiceBuilder serviceBuilder = QLService.builder();
          ServiceController.populateService(service, serviceBuilder);

          deployment.service(serviceBuilder.build());
        }

        if (qlQuery.isEnvironmentRequested()) {
          // TODO: RBAC this
          deployment.environment(environmentBuilder.build());
        }

        connectionBuilder.node(deployment.build());
      }
    }

    return connectionBuilder.pageInfo(pageInfoBuilder.build()).build();
  }
}
