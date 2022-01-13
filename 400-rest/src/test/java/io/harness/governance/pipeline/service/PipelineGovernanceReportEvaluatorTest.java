/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.governance.pipeline.service;

import static io.harness.rule.OwnerRule.UJJAWAL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.ff.FeatureFlagService;
import io.harness.governance.pipeline.enforce.PipelineReportCard;
import io.harness.governance.pipeline.service.model.PipelineGovernanceConfig;
import io.harness.governance.pipeline.service.model.Restriction;
import io.harness.governance.pipeline.service.model.Restriction.RestrictionType;
import io.harness.governance.pipeline.service.model.Tag;
import io.harness.persistence.UuidAccess;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.HarnessTagLink;
import software.wings.beans.Pipeline;
import software.wings.service.intfc.HarnessTagService;
import software.wings.service.intfc.PipelineService;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

public class PipelineGovernanceReportEvaluatorTest extends WingsBaseTest {
  @Mock private HarnessTagService harnessTagService;
  @Mock private PipelineGovernanceService pipelineGovernanceService;
  @Mock private PipelineService pipelineService;
  @Mock private FeatureFlagService featureFlagService;

  @Inject @InjectMocks private PipelineGovernanceReportEvaluator pipelineGovernanceReportEvaluator;

  private String SOME_ACCOUNT_ID = "some-account-id-" + PipelineGovernanceReportEvaluatorTest.class.getSimpleName();

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testIsConfigValidForApp() {
    PageResponse<HarnessTagLink> pageResponse = new PageResponse<>();
    pageResponse.setResponse(Arrays.asList(tagLink("color", "red"), tagLink("env", "prod")));
    when(harnessTagService.fetchTagsForEntity(Mockito.eq(SOME_ACCOUNT_ID), Mockito.any(UuidAccess.class)))
        .thenReturn(pageResponse);

    final List<Restriction> restrictions = new LinkedList<>();
    restrictions.add(new Restriction(
        RestrictionType.APP_BASED, Collections.singletonList("restricted-app-id"), Collections.emptyList()));
    boolean configValidForApp =
        pipelineGovernanceReportEvaluator.isConfigValidForApp(SOME_ACCOUNT_ID, restrictions, "some-app-id");
    assertThat(configValidForApp).as("should fail because appId is not present under restrictions").isFalse();

    restrictions.clear();
    restrictions.add(new Restriction(
        RestrictionType.APP_BASED, Collections.singletonList("restricted-app-id"), Collections.emptyList()));
    configValidForApp =
        pipelineGovernanceReportEvaluator.isConfigValidForApp(SOME_ACCOUNT_ID, restrictions, "restricted-app-id");
    assertThat(configValidForApp).as("should pass true because appId present under restrictions").isTrue();

    restrictions.clear();
    restrictions.add(new Restriction(
        RestrictionType.APP_BASED, Collections.singletonList("restricted-app-id"), Collections.emptyList()));
    restrictions.add(new Restriction(
        RestrictionType.TAG_BASED, Collections.emptyList(), Collections.singletonList(new Tag("color", "red"))));
    configValidForApp =
        pipelineGovernanceReportEvaluator.isConfigValidForApp(SOME_ACCOUNT_ID, restrictions, "some-app-id");
    assertThat(configValidForApp).as("should pass because tags under restrictions match tags of entity").isTrue();

    restrictions.clear();
    restrictions.add(new Restriction(
        RestrictionType.APP_BASED, Collections.singletonList("restricted-app-id"), Collections.emptyList()));
    restrictions.add(new Restriction(
        RestrictionType.TAG_BASED, Collections.emptyList(), Collections.singletonList(new Tag("color", "blue"))));
    configValidForApp =
        pipelineGovernanceReportEvaluator.isConfigValidForApp(SOME_ACCOUNT_ID, restrictions, "some-app-id");
    assertThat(configValidForApp)
        .as("should fail because tags under restrictions do NOT match tags of entity")
        .isFalse();

    restrictions.clear();
    configValidForApp =
        pipelineGovernanceReportEvaluator.isConfigValidForApp(SOME_ACCOUNT_ID, restrictions, "some-app-id");
    assertThat(configValidForApp).as("should pass because there are no restrictions").isTrue();
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testGetPipelineReportCard_emptyIfNoEnabledStandards() {
    String appId = "some-app-id";
    String pipelineId = "some-pipeline-id";

    final boolean standardEnabled = false;
    final PipelineGovernanceConfig config = new PipelineGovernanceConfig(null, SOME_ACCOUNT_ID, "some-name",
        "some-desc", Collections.emptyList(), Collections.emptyList(), standardEnabled);
    when(pipelineGovernanceService.list(SOME_ACCOUNT_ID)).thenReturn(Collections.singletonList(config));

    final Pipeline mockPipeline = Pipeline.builder().name("some-pipeline").build();
    when(pipelineService.readPipelineWithResolvedVariables(Mockito.eq(appId), Mockito.eq(pipelineId), Mockito.anyMap()))
        .thenReturn(mockPipeline);

    final List<PipelineReportCard> pipelineReports =
        pipelineGovernanceReportEvaluator.getPipelineReportCard(SOME_ACCOUNT_ID, appId, pipelineId);
    assertThat(pipelineReports).isEmpty();
  }

  private HarnessTagLink tagLink(String key, String value) {
    return HarnessTagLink.builder().key(key).value(value).build();
  }
}
