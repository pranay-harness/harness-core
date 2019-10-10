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
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.app.MainConfiguration;
import software.wings.beans.Application;
import software.wings.beans.FeatureName;
import software.wings.search.entities.application.ApplicationView;
import software.wings.search.framework.SearchResponse;
import software.wings.service.intfc.FeatureFlagService;

import java.util.List;

@Slf4j
public class ApplicationSearchEntitySyncTest extends AbstractFunctionalTest {
  @Inject private FeatureFlagService featureFlagService;
  @Inject private MainConfiguration mainConfiguration;
  private static final Retry retry = new Retry(10, 5000);
  private final String APP_NAME = "SyncTestApplication" + System.currentTimeMillis();
  private final String EDITED_APP_NAME = APP_NAME + "_Edited";
  private Application application;

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
  }

  @Test
  @Owner(emails = UTKARSH)
  @Category(FunctionalTests.class)
  public void testApplicationCRUDSync() {
    if (!featureFlagService.isGlobalEnabled(FeatureName.SEARCH) || !mainConfiguration.isSearchEnabled()) {
      return;
    }

    BooleanMatcher booleanMatcher = new BooleanMatcher();
    retry.executeWithRetry(this ::isApplicationInSearchResponse, booleanMatcher, true);
    logger.info("New application with id {} and name {} synced.", application.getUuid(), application.getName());

    application.setName(EDITED_APP_NAME);
    application = ApplicationRestUtils.updateApplication(
        bearerToken, application, application.getUuid(), application.getAccountId());

    assertThat(application).isNotNull();
    assertThat(application.getUuid()).isNotNull();
    assertThat(application.getName()).isEqualTo(EDITED_APP_NAME);

    retry.executeWithRetry(this ::isApplicationInSearchResponse, booleanMatcher, true);
    logger.info("Updated application with id {} and name {} synced", application.getUuid(), application.getName());

    int statusCode =
        ApplicationRestUtils.deleteApplication(bearerToken, application.getUuid(), application.getAccountId());
    assertThat(statusCode).isEqualTo(HttpStatus.SC_OK);

    retry.executeWithRetry(this ::isApplicationInSearchResponse, booleanMatcher, false);
  }

  private boolean isApplicationInSearchResponse() {
    boolean applicationFound = false;

    SearchResponse searchResponse =
        SearchRestUtils.search(bearerToken, application.getAccountId(), application.getName());

    List<ApplicationView> applicationViews = searchResponse.getApplications();

    for (ApplicationView applicationView : applicationViews) {
      if (applicationView.getId().equals(application.getUuid())
          && applicationView.getName().equals(application.getName())) {
        applicationFound = true;
        break;
      }
    }
    return applicationFound;
  }
}