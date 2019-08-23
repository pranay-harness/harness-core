package software.wings.graphql.datafetcher.application;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Application;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.schema.query.QLApplicationQueryParameters;
import software.wings.graphql.schema.type.QLApplication;
import software.wings.graphql.schema.type.QLApplication.QLApplicationBuilder;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

@Slf4j
public class ApplicationDataFetcher extends AbstractObjectDataFetcher<QLApplication, QLApplicationQueryParameters> {
  public static final String APP_DOES_NOT_EXIST_MSG = "Application does not exist";
  @Inject HPersistence persistence;

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  public QLApplication fetch(QLApplicationQueryParameters qlQuery, String accountId) {
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
      throw new InvalidRequestException(APP_DOES_NOT_EXIST_MSG, WingsException.USER);
    }

    final QLApplicationBuilder builder = QLApplication.builder();
    ApplicationController.populateApplication(application, builder);
    return builder.build();
  }
}
