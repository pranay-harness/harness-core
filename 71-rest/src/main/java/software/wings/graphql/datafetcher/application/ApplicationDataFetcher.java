package software.wings.graphql.datafetcher.application;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Application;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.schema.query.QLApplicationQueryParameters;
import software.wings.graphql.schema.type.QLApplication;
import software.wings.graphql.schema.type.QLApplication.QLApplicationBuilder;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

@Slf4j
public class ApplicationDataFetcher extends AbstractDataFetcher<QLApplication, QLApplicationQueryParameters> {
  @Inject HPersistence persistence;

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  public QLApplication fetch(QLApplicationQueryParameters qlQuery) {
    Application application = null;
    if (qlQuery.getApplicationId() != null) {
      application = persistence.get(Application.class, qlQuery.getApplicationId());
    } else if (qlQuery.getExecutionId() != null) {
      // TODO: add this to in memory cache
      final String applicationId = persistence.createQuery(WorkflowExecution.class)
                                       .filter(WorkflowExecutionKeys.uuid, qlQuery.getExecutionId())
                                       .project(WorkflowExecutionKeys.appId, true)
                                       .get()
                                       .getAppId();

      application = persistence.get(Application.class, applicationId);
    }
    if (application == null) {
      throw new InvalidRequestException("Application does not exist", WingsException.USER);
    }

    final QLApplicationBuilder builder = QLApplication.builder();
    ApplicationController.populateApplication(application, builder);
    return builder.build();
  }
}
