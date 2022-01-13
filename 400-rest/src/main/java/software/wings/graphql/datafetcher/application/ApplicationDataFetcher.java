/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.application;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;

import software.wings.beans.Application;
import software.wings.beans.Application.ApplicationKeys;
import software.wings.beans.User;
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.schema.query.QLApplicationQueryParameters;
import software.wings.graphql.schema.type.QLApplication;
import software.wings.graphql.schema.type.QLApplication.QLApplicationBuilder;
import software.wings.security.PermissionAttribute;
import software.wings.security.UserThreadLocal;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.AuthService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class ApplicationDataFetcher extends AbstractObjectDataFetcher<QLApplication, QLApplicationQueryParameters> {
  private static final String APP_DOES_NOT_EXIST_MSG = "Application does not exist";
  @Inject HPersistence persistence;
  @Inject AppService appService;
  @Inject AuthService authService;

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

    final User user = UserThreadLocal.get();
    if (user != null) {
      authService.authorizeAppAccess(accountId, application.getUuid(), user, PermissionAttribute.Action.READ);
    }

    final QLApplicationBuilder builder = QLApplication.builder();
    ApplicationController.populateQLApplication(application, builder);
    return builder.build();
  }
}
