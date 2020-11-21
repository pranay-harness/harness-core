package software.wings.graphql.datafetcher.application;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;

import software.wings.beans.Application;
import software.wings.beans.Application.ApplicationKeys;
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.schema.query.QLApplicationQueryParameters;
import software.wings.graphql.schema.type.QLApplication;
import software.wings.graphql.schema.type.QLApplication.QLApplicationBuilder;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AppService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApplicationDataFetcher extends AbstractObjectDataFetcher<QLApplication, QLApplicationQueryParameters> {
  private static final String APP_DOES_NOT_EXIST_MSG = "Application does not exist";
  @Inject HPersistence persistence;
  @Inject AppService appService;

  @Override
  @AuthRule(permissionType = LOGGED_IN)
  public QLApplication fetch(QLApplicationQueryParameters qlQuery, String accountId) {
    Application application = null;
    if (qlQuery.getApplicationId() != null) {
      application = persistence.get(Application.class, qlQuery.getApplicationId());
    }
    if (qlQuery.getName() != null) {
      try (HIterator<Application> iterator = new HIterator<>(persistence.createQuery(Application.class)
                                                                 .filter(ApplicationKeys.name, qlQuery.getName())
                                                                 .filter(ApplicationKeys.accountId, accountId)
                                                                 .fetch())) {
        if (iterator.hasNext()) {
          application = iterator.next();
        }
      }
    }
    if (application == null) {
      throw new InvalidRequestException(APP_DOES_NOT_EXIST_MSG, WingsException.USER);
    }

    final QLApplicationBuilder builder = QLApplication.builder();
    ApplicationController.populateQLApplication(application, builder);
    return builder.build();
  }
}
