package io.harness.governance.pipeline.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.governance.pipeline.service.model.Restriction;
import io.harness.governance.pipeline.service.model.Restriction.RestrictionType;
import io.harness.governance.pipeline.service.model.Tag;
import io.harness.persistence.UuidAccess;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.beans.HarnessTagLink;
import software.wings.service.intfc.HarnessTagService;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class PipelineGovernanceReportEvaluatorTest extends WingsBaseTest {
  @Mock private HarnessTagService harnessTagService;

  @Inject @InjectMocks private PipelineGovernanceReportEvaluator pipelineGovernanceReportEvaluator;

  private String SOME_ACCOUNT_ID = "some-account-id-" + PipelineGovernanceReportEvaluatorTest.class.getSimpleName();

  @Test
  @Category(UnitTests.class)
  public void testIsConfigValidForApp() {
    PageResponse<HarnessTagLink> pageResponse = new PageResponse<>();
    pageResponse.setResponse(Arrays.asList(tagLink("color", "red"), tagLink("env", "prod")));
    Mockito.when(harnessTagService.fetchTagsForEntity(Mockito.eq(SOME_ACCOUNT_ID), Mockito.any(UuidAccess.class)))
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

  private HarnessTagLink tagLink(String key, String value) {
    return HarnessTagLink.builder().key(key).value(value).build();
  }
}
