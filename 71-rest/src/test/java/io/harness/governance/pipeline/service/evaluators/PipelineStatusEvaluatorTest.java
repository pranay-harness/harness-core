package io.harness.governance.pipeline.service.evaluators;

import static io.harness.rule.OwnerRule.UJJAWAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.governance.pipeline.enforce.GovernanceRuleStatus;
import io.harness.governance.pipeline.service.model.MatchType;
import io.harness.governance.pipeline.service.model.PipelineGovernanceRule;
import io.harness.governance.pipeline.service.model.Tag;
import io.harness.persistence.UuidAccess;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.beans.HarnessTagLink;
import software.wings.beans.Pipeline;
import software.wings.service.intfc.HarnessTagService;

import java.util.Arrays;
import java.util.List;

public class PipelineStatusEvaluatorTest extends WingsBaseTest {
  @Mock private HarnessTagService harnessTagService;
  @Inject @InjectMocks private PipelineStatusEvaluator pipelineStatusEvaluator;

  private static final String SOME_ACCOUNT_ID = "some-account-id-" + PipelineStatusEvaluatorTest.class.getSimpleName();

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testStatus() {
    PageResponse<HarnessTagLink> pageResponse = new PageResponse<>();
    pageResponse.setResponse(Arrays.asList(HarnessTagLink.builder().key("tag1").build()));
    when(harnessTagService.fetchTagsForEntity(Mockito.eq(SOME_ACCOUNT_ID), Mockito.any(UuidAccess.class)))
        .thenReturn(pageResponse);

    Pipeline pipeline = new Pipeline();
    pipeline.setAccountId(SOME_ACCOUNT_ID);
    pipeline.setName("test-workflow");
    pipeline.setUuid("some-uuid");

    List<Tag> tags = Arrays.asList(new Tag("tag1", null), new Tag("tag2", "tag2val"));
    PipelineGovernanceRule rule = new PipelineGovernanceRule(tags, MatchType.ALL, 10, "note");
    GovernanceRuleStatus status = pipelineStatusEvaluator.status(SOME_ACCOUNT_ID, pipeline, rule);
    assertThat(status.isTagsIncluded()).isFalse();

    tags = Arrays.asList(new Tag("tag1", null), new Tag("tag2", "tag2val"));
    rule = new PipelineGovernanceRule(tags, MatchType.ANY, 10, "note");
    status = pipelineStatusEvaluator.status(SOME_ACCOUNT_ID, pipeline, rule);
    assertThat(status.isTagsIncluded()).isTrue();
  }
}
