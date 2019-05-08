package software.wings.graphql.datafetcher.environment;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Environment;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.schema.query.QLEnvironmentQueryParameters;
import software.wings.graphql.schema.type.QLEnvironment;
import software.wings.graphql.schema.type.QLEnvironment.QLEnvironmentBuilder;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

@Slf4j
public class EnvironmentDataFetcher extends AbstractDataFetcher<QLEnvironment, QLEnvironmentQueryParameters> {
  @Inject HPersistence persistence;

  @Override
  @AuthRule(permissionType = PermissionType.ENV, action = Action.READ)
  public QLEnvironment fetch(QLEnvironmentQueryParameters qlQuery) {
    Environment environment = persistence.get(Environment.class, qlQuery.getEnvironmentId());
    if (environment == null) {
      throw new InvalidRequestException("Environment does not exist", WingsException.USER);
    }

    final QLEnvironmentBuilder builder = QLEnvironment.builder();
    EnvironmentController.populateEnvironment(environment, builder);
    return builder.build();
  }
}
