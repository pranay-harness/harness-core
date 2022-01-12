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
import io.harness.testframework.restutils.PipelineRestUtils;
import io.harness.testframework.restutils.SearchRestUtils;

import software.wings.app.MainConfiguration;
import software.wings.beans.Application;
import software.wings.beans.Pipeline;
import software.wings.search.entities.pipeline.PipelineSearchEntity;
import software.wings.search.framework.SearchResult;
import software.wings.search.framework.SearchResults;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class PipelineSearchEntitySyncTest extends AbstractFunctionalTest {
  @Inject private FeatureFlagService featureFlagService;
  @Inject private MainConfiguration mainConfiguration;
  private static final Retry retry = new Retry(10, 5000);
  private final String APP_NAME = "SyncTestApplication" + System.currentTimeMillis();
  private final String PIPELINE_NAME = "SyncTestPipeline" + System.currentTimeMillis();
  private final String EDITED_PIPELINE_NAME = PIPELINE_NAME + "_Edited";
  private Application application;
  private Pipeline pipeline;

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

    pipeline = new Pipeline();
    pipeline.setAppId(application.getUuid());
    pipeline.setName(PIPELINE_NAME);

    pipeline = PipelineRestUtils.createPipeline(pipeline.getAppId(), pipeline, getAccount().getUuid(), bearerToken);
    assertThat(pipeline).isNotNull();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(FunctionalTests.class)
  public void testPipelineCRUDSync() {
    if (isSearchDisabled()) {
      return;
    }

    BooleanMatcher booleanMatcher = new BooleanMatcher();
    retry.executeWithRetry(this::isPipelineInSearchResponse, booleanMatcher, true);
    log.info("New pipeline with id {} and name {} synced.", pipeline.getUuid(), pipeline.getName());

    pipeline.setName(EDITED_PIPELINE_NAME);
    pipeline = PipelineRestUtils.updatePipeline(application.getUuid(), pipeline, bearerToken);

    assertThat(pipeline).isNotNull();
    assertThat(pipeline.getName()).isEqualTo(EDITED_PIPELINE_NAME);

    retry.executeWithRetry(this::isPipelineInSearchResponse, booleanMatcher, true);
    log.info("Pipeline update with id {} and name {} synced.", pipeline.getUuid(), pipeline.getName());

    int statusCode = PipelineRestUtils.deletePipeline(pipeline.getAppId(), pipeline.getUuid(), bearerToken);
    assertThat(statusCode).isEqualTo(HttpStatus.SC_OK);

    retry.executeWithRetry(this::isPipelineInSearchResponse, booleanMatcher, false);
    log.info("Pipeline with id {} deleted", pipeline.getUuid());

    ApplicationRestUtils.deleteApplication(bearerToken, application.getUuid(), application.getAccountId());
  }

  private boolean isPipelineInSearchResponse() {
    boolean pipelineFound = false;

    SearchResults searchResults = SearchRestUtils.search(bearerToken, application.getAccountId(), pipeline.getName());

    for (SearchResult pipelineSearchResult : searchResults.getSearchResults().get(PipelineSearchEntity.TYPE)) {
      if (pipelineSearchResult.getId().equals(pipeline.getUuid())
          && pipelineSearchResult.getName().equals(pipeline.getName())) {
        pipelineFound = true;
        break;
      }
    }
    return pipelineFound;
  }

  private boolean isSearchDisabled() {
    return !featureFlagService.isEnabled(FeatureName.SEARCH_REQUEST, getAccount().getUuid())
        || !mainConfiguration.isSearchEnabled();
  }
}
