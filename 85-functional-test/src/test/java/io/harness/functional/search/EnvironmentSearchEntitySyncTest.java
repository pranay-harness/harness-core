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
import io.harness.testframework.restutils.EnvironmentRestUtils;
import io.harness.testframework.restutils.SearchRestUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.app.MainConfiguration;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.FeatureName;
import software.wings.search.entities.environment.EnvironmentView;
import software.wings.search.framework.SearchResponse;
import software.wings.service.intfc.FeatureFlagService;

import java.util.List;

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
    if (!featureFlagService.isGlobalEnabled(FeatureName.SEARCH) || !mainConfiguration.isSearchEnabled()) {
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
  @Owner(emails = UTKARSH)
  @Category(FunctionalTests.class)
  public void testEnvironmentCRUDSync() {
    if (!featureFlagService.isGlobalEnabled(FeatureName.SEARCH) || !mainConfiguration.isSearchEnabled()) {
      return;
    }

    BooleanMatcher booleanMatcher = new BooleanMatcher();
    retry.executeWithRetry(this ::isEnvironmentInSearchResponse, booleanMatcher, true);
    logger.info("New environment with id {} and name {} synced.", environment.getUuid(), environment.getName());

    environment.setName(EDITED_ENVIRONMENT_NAME);
    environment = EnvironmentRestUtils.updateEnvironment(
        bearerToken, application.getAccountId(), environment.getAppId(), environment);

    assertThat(environment).isNotNull();
    assertThat(environment.getName()).isEqualTo(EDITED_ENVIRONMENT_NAME);

    retry.executeWithRetry(this ::isEnvironmentInSearchResponse, booleanMatcher, true);
    logger.info("Environment update with id {} and name {} synced.", environment.getUuid(), environment.getName());

    int statusCode = EnvironmentRestUtils.deleteEnvironment(
        bearerToken, environment.getAppId(), environment.getAccountId(), environment.getUuid());
    assertThat(statusCode).isEqualTo(HttpStatus.SC_OK);

    retry.executeWithRetry(this ::isEnvironmentInSearchResponse, booleanMatcher, false);
    logger.info("Environment with id {} deleted", environment.getUuid());

    ApplicationRestUtils.deleteApplication(bearerToken, application.getUuid(), application.getAccountId());
  }

  private boolean isEnvironmentInSearchResponse() {
    boolean environmentFound = false;

    SearchResponse searchResponse =
        SearchRestUtils.search(bearerToken, application.getAccountId(), environment.getName());

    List<EnvironmentView> environmentViews = searchResponse.getEnvironments();

    for (EnvironmentView environmentView : environmentViews) {
      if (environmentView.getId().equals(environment.getUuid())
          && environmentView.getName().equals(environment.getName())
          && environmentView.getAppId().equals(environment.getAppId())
          && environmentView.getAppName().equals(application.getName())) {
        environmentFound = true;
        break;
      }
    }
    return environmentFound;
  }
}