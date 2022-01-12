/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.search;

import static io.harness.rule.OwnerRule.UTKARSH;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.FeatureName;
import io.harness.category.element.FunctionalTests;
import io.harness.ff.FeatureFlagService;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rule.Owner;
import io.harness.testframework.framework.Retry;
import io.harness.testframework.framework.matchers.BooleanMatcher;
import io.harness.testframework.restutils.ApplicationRestUtils;
import io.harness.testframework.restutils.SearchRestUtils;
import io.harness.testframework.restutils.ServiceRestUtils;

import software.wings.app.MainConfiguration;
import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.search.entities.service.ServiceSearchEntity;
import software.wings.search.framework.SearchResult;
import software.wings.search.framework.SearchResults;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class ServiceSearchEntitySyncTest extends AbstractFunctionalTest {
  @Inject private FeatureFlagService featureFlagService;
  @Inject private MainConfiguration mainConfiguration;
  private static final Retry retry = new Retry(10, 5000);
  private final String APP_NAME = "SyncTestApplication" + System.currentTimeMillis();
  private final String SERVICE_NAME = "SyncTestService" + System.currentTimeMillis();
  private final String EDITED_SERVICE_NAME = SERVICE_NAME + "_Edited";
  private Application application;
  private Service service;

  @Before
  public void setUp() {
    if (isSearchDisabled()) {
      return;
    }

    application = new Application();
    application.setName(APP_NAME);

    application = ApplicationRestUtils.createApplication(bearerToken, getAccount(), application);
    assertThat(application).isNotNull();

    service = new Service();
    service.setAppId(application.getUuid());
    service.setName(SERVICE_NAME);
    service.setK8sV2(true);

    String serviceUuid =
        ServiceRestUtils.createService(bearerToken, getAccount().getUuid(), application.getUuid(), service);
    assertThat(serviceUuid).isNotNull();
    service.setUuid(serviceUuid);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(FunctionalTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void testServiceCRUDSync() {
    if (isSearchDisabled()) {
      return;
    }

    BooleanMatcher booleanMatcher = new BooleanMatcher();
    retry.executeWithRetry(this::isServiceInSearchResponse, booleanMatcher, true);
    log.info("New service with id {} and name {} synced.", service.getUuid(), service.getName());

    service.setName(EDITED_SERVICE_NAME);
    service = ServiceRestUtils.updateService(bearerToken, application.getAccountId(), service.getAppId(), service);

    assertThat(service).isNotNull();
    assertThat(service.getName()).isEqualTo(EDITED_SERVICE_NAME);

    retry.executeWithRetry(this::isServiceInSearchResponse, booleanMatcher, true);
    log.info("Service update with id {} and name {} synced.", service.getUuid(), service.getName());

    int statusCode = ServiceRestUtils.deleteService(bearerToken, service.getAppId(), service.getUuid());
    assertThat(statusCode).isEqualTo(HttpStatus.SC_OK);

    retry.executeWithRetry(this::isServiceInSearchResponse, booleanMatcher, false);
    log.info("Service with id {} deleted", service.getUuid());

    ApplicationRestUtils.deleteApplication(bearerToken, application.getUuid(), application.getAccountId());
  }

  private boolean isServiceInSearchResponse() {
    boolean serviceFound = false;

    SearchResults searchResults = SearchRestUtils.search(bearerToken, application.getAccountId(), service.getName());

    for (SearchResult serviceSearchResult : searchResults.getSearchResults().get(ServiceSearchEntity.TYPE)) {
      if (serviceSearchResult.getId().equals(service.getUuid())
          && serviceSearchResult.getName().equals(service.getName())) {
        serviceFound = true;
        break;
      }
    }
    return serviceFound;
  }

  private boolean isSearchDisabled() {
    return !featureFlagService.isEnabled(FeatureName.SEARCH_REQUEST, getAccount().getUuid())
        || !mainConfiguration.isSearchEnabled();
  }
}
