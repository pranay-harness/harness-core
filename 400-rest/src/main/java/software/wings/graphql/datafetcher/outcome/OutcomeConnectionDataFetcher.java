/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.outcome;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.Environment;
import software.wings.beans.WorkflowExecution;
import software.wings.graphql.datafetcher.AbstractConnectionDataFetcher;
import software.wings.graphql.datafetcher.environment.EnvironmentController;
import software.wings.graphql.datafetcher.execution.ExecutionController;
import software.wings.graphql.datafetcher.execution.WorkflowExecutionController;
import software.wings.graphql.schema.query.QLEnvironmentQueryParameters.QLEnvironmentQueryParametersKeys;
import software.wings.graphql.schema.query.QLExecutionQueryParameters.QLExecutionQueryParametersKeys;
import software.wings.graphql.schema.query.QLOutcomesQueryParameters;
import software.wings.graphql.schema.query.QLServiceQueryParameters.QLServiceQueryParametersKeys;
import software.wings.graphql.schema.type.QLDeploymentOutcome;
import software.wings.graphql.schema.type.QLDeploymentOutcome.QLDeploymentOutcomeBuilder;
import software.wings.graphql.schema.type.QLEnvironment;
import software.wings.graphql.schema.type.QLEnvironment.QLEnvironmentBuilder;
import software.wings.graphql.schema.type.QLOutcomeConnection;
import software.wings.graphql.schema.type.QLOutcomeConnection.QLOutcomeConnectionBuilder;
import software.wings.graphql.schema.type.QLPageInfo;
import software.wings.graphql.schema.type.QLPageInfo.QLPageInfoBuilder;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class OutcomeConnectionDataFetcher
    extends AbstractConnectionDataFetcher<QLOutcomeConnection, QLOutcomesQueryParameters> {
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
      if (environment != null) {
        EnvironmentController.populateEnvironment(environment, environmentBuilder);
      }

      for (ElementExecutionSummary summary : execution.getServiceExecutionSummaries()) {
        QLDeploymentOutcomeBuilder deployment = QLDeploymentOutcome.builder().context(
            ImmutableMap.<String, Object>builder()
                .put(QLExecutionQueryParametersKeys.executionId, qlQuery.getExecutionId())
                .put(QLServiceQueryParametersKeys.serviceId, summary.getContextElement().getUuid())
                .put(QLEnvironmentQueryParametersKeys.environmentId, execution.getEnvId())
                .build());

        connectionBuilder.node(deployment.build());
      }
    }

    return connectionBuilder.pageInfo(pageInfoBuilder.build()).build();
  }
}
