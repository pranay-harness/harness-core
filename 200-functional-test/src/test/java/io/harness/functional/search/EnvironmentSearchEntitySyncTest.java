/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.search;

import static io.harness.rule.OwnerRule.UTKARSH;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.EnvironmentType;
import io.harness.beans.FeatureName;
import io.harness.category.element.FunctionalTests;
import io.harness.ff.FeatureFlagService;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rule.Owner;
import io.harness.testframework.framework.Retry;
import io.harness.testframework.framework.matchers.BooleanMatcher;
import io.harness.testframework.restutils.ApplicationRestUtils;
import io.harness.testframework.restutils.EnvironmentRestUtils;
import io.harness.testframework.restutils.SearchRestUtils;

import software.wings.app.MainConfiguration;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.search.entities.environment.EnvironmentSearchEntity;
import software.wings.search.framework.SearchResult;
import software.wings.search.framework.SearchResults;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class EnvironmentSearchEntitySyncTest extends AbstractFunctionalTest {
  @Inject private FeatureFlagService featureFlagService;
  @Inject private MainConfiguration mainConfiguration;
  private static final Retry retry = new Retry(10, 5000);
  private final String APP_NAME = "SyncTestApplication" + System.currentTimeMillis();
  private final String ENVIRONMENT_NAME = "SyncTestEnvironment" + System.currentTimeMillis();
  private final String EDITED_ENVIRONMENT_NAME = ENVIRONMENT_NAME + "_Edited";
  private Application application;
  private Environment environment;

  @Before
  public void setUp() {
    if (isSearchDisabled()) {
      return;
    }

    application = new Application();
    application.setAccountId(getAccount().getUuid());
    application.setName(APP_NAME);

    application = ApplicationRestUtils.createApplication(bearerToken, getAccount(), application);
    assertThat(application).isNotNull();

    environment = new Environment();
    environment.setAppId(application.getUuid());
    environment.setName(ENVIRONMENT_NAME);
    environment.setEnvironmentType(EnvironmentType.PROD);

    environment = EnvironmentRestUtils.createEnvironment(bearerToken, getAccount(), application.getUuid(), environment);
    assertThat(environment).isNotNull();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(FunctionalTests.class)
  public void testEnvironmentCRUDSync() {
    if (isSearchDisabled()) {
      return;
    }

    BooleanMatcher booleanMatcher = new BooleanMatcher();
    retry.executeWithRetry(this::isEnvironmentInSearchResponse, booleanMatcher, true);
    log.info("New environment with id {} and name {} synced.", environment.getUuid(), environment.getName());

    environment.setName(EDITED_ENVIRONMENT_NAME);
    environment = EnvironmentRestUtils.updateEnvironment(
        bearerToken, application.getAccountId(), environment.getAppId(), environment);

    assertThat(environment).isNotNull();
    assertThat(environment.getName()).isEqualTo(EDITED_ENVIRONMENT_NAME);

    retry.executeWithRetry(this::isEnvironmentInSearchResponse, booleanMatcher, true);
    log.info("Environment update with id {} and name {} synced.", environment.getUuid(), environment.getName());

    int statusCode = EnvironmentRestUtils.deleteEnvironment(
        bearerToken, environment.getAppId(), environment.getAccountId(), environment.getUuid());
    assertThat(statusCode).isEqualTo(HttpStatus.SC_OK);

    retry.executeWithRetry(this::isEnvironmentInSearchResponse, booleanMatcher, false);
    log.info("Environment with id {} deleted", environment.getUuid());

    ApplicationRestUtils.deleteApplication(bearerToken, application.getUuid(), application.getAccountId());
  }

  private boolean isEnvironmentInSearchResponse() {
    boolean environmentFound = false;
    SearchResults searchResults =
        SearchRestUtils.search(bearerToken, application.getAccountId(), environment.getName());

    for (SearchResult environmentSearchResult : searchResults.getSearchResults().get(EnvironmentSearchEntity.TYPE)) {
      if (environmentSearchResult.getId().equals(environment.getUuid())
          && environmentSearchResult.getName().equals(environment.getName())) {
        environmentFound = true;
        break;
      }
    }
    return environmentFound;
  }

  private boolean isSearchDisabled() {
    return !featureFlagService.isEnabled(FeatureName.SEARCH_REQUEST, getAccount().getUuid())
        || !mainConfiguration.isSearchEnabled();
  }
}
