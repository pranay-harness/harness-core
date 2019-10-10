package io.harness.functional.search;

import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rule.OwnerRule.Owner;
import io.harness.testframework.framework.Retry;
import io.harness.testframework.framework.matchers.BooleanMatcher;
import io.harness.testframework.restutils.ApplicationRestUtils;
import io.harness.testframework.restutils.SearchRestUtils;
import io.harness.testframework.restutils.ServiceRestUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.app.MainConfiguration;
import software.wings.beans.Application;
import software.wings.beans.FeatureName;
import software.wings.beans.Service;
import software.wings.search.entities.service.ServiceView;
import software.wings.search.framework.SearchResponse;
import software.wings.service.intfc.FeatureFlagService;

import java.util.List;

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
    if (!featureFlagService.isGlobalEnabled(FeatureName.SEARCH) || !mainConfiguration.isSearchEnabled()) {
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
  @Owner(emails = UTKARSH)
  @Category(FunctionalTests.class)
  public void testServiceCRUDSync() {
    if (!featureFlagService.isGlobalEnabled(FeatureName.SEARCH) || !mainConfiguration.isSearchEnabled()) {
      return;
    }

    BooleanMatcher booleanMatcher = new BooleanMatcher();
    retry.executeWithRetry(this ::isServiceInSearchResponse, booleanMatcher, true);
    logger.info("New service with id {} and name {} synced.", service.getUuid(), service.getName());

    service.setName(EDITED_SERVICE_NAME);
    service = ServiceRestUtils.updateService(bearerToken, application.getAccountId(), service.getAppId(), service);

    assertThat(service).isNotNull();
    assertThat(service.getName()).isEqualTo(EDITED_SERVICE_NAME);

    retry.executeWithRetry(this ::isServiceInSearchResponse, booleanMatcher, true);
    logger.info("Service update with id {} and name {} synced.", service.getUuid(), service.getName());

    int statusCode = ServiceRestUtils.deleteService(bearerToken, service.getAppId(), service.getUuid());
    assertThat(statusCode).isEqualTo(HttpStatus.SC_OK);

    retry.executeWithRetry(this ::isServiceInSearchResponse, booleanMatcher, false);
    logger.info("Service with id {} deleted", service.getUuid());

    ApplicationRestUtils.deleteApplication(bearerToken, application.getUuid(), application.getAccountId());
  }

  private boolean isServiceInSearchResponse() {
    boolean serviceFound = false;

    SearchResponse searchResponse = SearchRestUtils.search(bearerToken, application.getAccountId(), service.getName());

    List<ServiceView> serviceViews = searchResponse.getServices();

    for (ServiceView serviceView : serviceViews) {
      if (serviceView.getId().equals(service.getUuid()) && serviceView.getName().equals(service.getName())
          && serviceView.getAppId().equals(service.getAppId())
          && serviceView.getAppName().equals(application.getName())) {
        serviceFound = true;
        break;
      }
    }
    return serviceFound;
  }
}