package io.harness.ngtriggers.eventmapper.filters.impl;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.ngtriggers.Constants.CHANGED_FILES;
import static io.harness.ngtriggers.conditionchecker.ConditionOperator.EQUALS;
import static io.harness.ngtriggers.conditionchecker.ConditionOperator.REGEX;
import static io.harness.rule.OwnerRule.ADWAIT;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.scm.WebhookPayloadData;
import io.harness.ngtriggers.beans.source.WebhookTriggerType;
import io.harness.ngtriggers.beans.source.webhook.v2.TriggerEventDataCondition;
import io.harness.ngtriggers.eventmapper.filters.dto.FilterRequestData;
import io.harness.ngtriggers.mapper.NGTriggerElementMapper;
import io.harness.ngtriggers.service.NGTriggerService;
import io.harness.ngtriggers.utils.TaskExecutionUtils;
import io.harness.product.ci.scm.proto.Commit;
import io.harness.product.ci.scm.proto.ParseWebhookResponse;
import io.harness.product.ci.scm.proto.PullRequestHook;
import io.harness.product.ci.scm.proto.PushHook;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;
import io.harness.utils.ConnectorUtils;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(PIPELINE)
public class TriggerFilePathConditionFilterTest extends CategoryTest {
  @Mock private TaskExecutionUtils taskExecutionUtils;
  @Mock private NGTriggerElementMapper ngTriggerElementMapper;
  @Mock private NGTriggerService ngTriggerService;
  @Mock private KryoSerializer kryoSerializer;
  @Mock private ConnectorUtils connectorUtils;
  @Inject @InjectMocks private FilepathTriggerFilter filter;
  private static List<NGTriggerEntity> triggerEntities;

  String pushPayload = "{\"commits\": [\n"
      + "  {\n"
      + "    \"id\": \"3a45ee02a55a29d696a2a1b0b923efa81523bb6c\",\n"
      + "    \"tree_id\": \"cc8524297287b55f07e38c56ecd43625f935252f\",\n"
      + "    \"distinct\": true,\n"
      + "    \"message\": \"nn\",\n"
      + "    \"timestamp\": \"2021-06-25T14:52:49-07:00\",\n"
      + "    \"url\": \"https://github.com/wings-software/cicddemo/commit/3a45ee02a55a29d696a2a1b0b923efa81523bb6c\",\n"
      + "    \"author\": {\n"
      + "      \"name\": \"Adwait Bhandare\",\n"
      + "      \"email\": \"adwait.bhandare@harness.io\",\n"
      + "      \"username\": \"adwaitabhandare\"\n"
      + "    },\n"
      + "    \"committer\": {\n"
      + "      \"name\": \"GitHub\",\n"
      + "      \"email\": \"noreply@github.com\",\n"
      + "      \"username\": \"web-flow\"\n"
      + "    },\n"
      + "    \"added\": [\n"
      + "      \"spec/manifest1.yml\"\n"
      + "    ],\n"
      + "    \"removed\": [\n"
      + "      \"File1_Removed.txt\"\n"
      + "    ],\n"
      + "    \"modified\": [\n"
      + "      \"values/value1.yml\"\n"
      + "    ]\n"
      + "  }, \n"
      + "  {\n"
      + "    \"id\": \"3a45ee02a55a29d696a2a1b0b923efa81523bb6c\",\n"
      + "    \"tree_id\": \"cc8524297287b55f07e38c56ecd43625f935252f\",\n"
      + "    \"distinct\": true,\n"
      + "    \"message\": \"nn\",\n"
      + "    \"timestamp\": \"2021-06-25T14:52:49-07:00\",\n"
      + "    \"url\": \"https://github.com/wings-software/cicddemo/commit/3a45ee02a55a29d696a2a1b0b923efa81523bb6c\",\n"
      + "    \"author\": {\n"
      + "      \"name\": \"Adwait Bhandare\",\n"
      + "      \"email\": \"adwait.bhandare@harness.io\",\n"
      + "      \"username\": \"adwaitabhandare\"\n"
      + "    },\n"
      + "    \"committer\": {\n"
      + "      \"name\": \"GitHub\",\n"
      + "      \"email\": \"noreply@github.com\",\n"
      + "      \"username\": \"web-flow\"\n"
      + "    },\n"
      + "    \"added\": [\n"
      + "      \"spec/manifest2.yml\"\n"
      + "    ],\n"
      + "    \"removed\": [\n"
      + "      \"File2_Removed.txt\"\n"
      + "    ],\n"
      + "    \"modified\": [\n"
      + "      \"values/value2.yml\"\n"
      + "    ]\n"
      + "  }\n"
      + "]}";

  @Before
  public void setUp() throws Exception {
    initMocks(this);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testPathFilterEvaluationNotNeeded() {
    FilterRequestData filterRequestData =
        FilterRequestData.builder().webhookPayloadData(WebhookPayloadData.builder().build()).build();
    assertThat(filter.pathFilterEvaluationNotNeeded(filterRequestData)).isTrue();

    filterRequestData.setWebhookPayloadData(
        WebhookPayloadData.builder()
            .originalEvent(TriggerWebhookEvent.builder()
                               .sourceRepoType(WebhookTriggerType.AWS_CODECOMMIT.getEntityMetadataName())
                               .build())
            .build());
    assertThat(filter.pathFilterEvaluationNotNeeded(filterRequestData)).isTrue();
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void evaluateFromPushPayload() {
    TriggerWebhookEvent triggerWebhookEvent =
        TriggerWebhookEvent.builder().payload(pushPayload).sourceRepoType("Github").build();

    FilterRequestData filterRequestData =
        FilterRequestData.builder()
            .webhookPayloadData(WebhookPayloadData.builder().originalEvent(triggerWebhookEvent).build())
            .build();

    TriggerEventDataCondition condition =
        TriggerEventDataCondition.builder().key(CHANGED_FILES).operator(EQUALS).value("spec/manifest1.yml").build();
    assertThat(filter.evaluateFromPushPayload(filterRequestData, condition)).isTrue();

    condition.setOperator(REGEX);
    condition.setValue("(^spec/manifest)[0-9](.yml1$)");
    assertThat(filter.evaluateFromPushPayload(filterRequestData, condition)).isFalse();
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void shouldEvaluateOnDelegate() {
    ParseWebhookResponse parseWebhookResponse =
        ParseWebhookResponse.newBuilder().setPr(PullRequestHook.newBuilder().build()).build();
    TriggerWebhookEvent triggerWebhookEvent =
        TriggerWebhookEvent.builder().payload("").sourceRepoType("Github").build();

    WebhookPayloadData webhookPayloadData = WebhookPayloadData.builder()
                                                .originalEvent(triggerWebhookEvent)
                                                .parseWebhookResponse(parseWebhookResponse)
                                                .build();
    FilterRequestData filterRequestData = FilterRequestData.builder().webhookPayloadData(webhookPayloadData).build();

    // PR
    assertThat(filter.shouldEvaluateOnDelegate(filterRequestData)).isTrue();

    // Push Github , commits < 20
    List<Commit> commits = Arrays.asList(Commit.newBuilder().build(), Commit.newBuilder().build());
    parseWebhookResponse =
        ParseWebhookResponse.newBuilder().setPush(PushHook.newBuilder().addAllCommits(commits).build()).build();
    webhookPayloadData = WebhookPayloadData.builder()
                             .originalEvent(triggerWebhookEvent)
                             .parseWebhookResponse(parseWebhookResponse)
                             .build();
    filterRequestData = FilterRequestData.builder().webhookPayloadData(webhookPayloadData).build();
    assertThat(filter.shouldEvaluateOnDelegate(filterRequestData)).isFalse();

    // Push gitlab , commits < 20
    parseWebhookResponse =
        ParseWebhookResponse.newBuilder().setPush(PushHook.newBuilder().addAllCommits(commits).build()).build();
    triggerWebhookEvent.setSourceRepoType("Gitlab");
    webhookPayloadData = WebhookPayloadData.builder()
                             .originalEvent(triggerWebhookEvent)
                             .parseWebhookResponse(parseWebhookResponse)
                             .build();
    filterRequestData = FilterRequestData.builder().webhookPayloadData(webhookPayloadData).build();
    assertThat(filter.shouldEvaluateOnDelegate(filterRequestData)).isFalse();

    // Push Bitbucket , commits < 20
    commits = emptyList();
    parseWebhookResponse =
        ParseWebhookResponse.newBuilder().setPush(PushHook.newBuilder().addAllCommits(commits).build()).build();
    triggerWebhookEvent.setSourceRepoType("Bitbucket");
    webhookPayloadData = WebhookPayloadData.builder()
                             .originalEvent(triggerWebhookEvent)
                             .parseWebhookResponse(parseWebhookResponse)
                             .build();
    filterRequestData = FilterRequestData.builder().webhookPayloadData(webhookPayloadData).build();
    assertThat(filter.shouldEvaluateOnDelegate(filterRequestData)).isTrue();

    commits = new ArrayList<>();
    Commit commit = Commit.newBuilder().build();
    for (int i = 0; i < 20; i++) {
      commits.add(commit);
    }
    // Push Github , commits > 20
    parseWebhookResponse =
        ParseWebhookResponse.newBuilder().setPush(PushHook.newBuilder().addAllCommits(commits).build()).build();
    triggerWebhookEvent.setSourceRepoType("github");
    webhookPayloadData = WebhookPayloadData.builder()
                             .originalEvent(triggerWebhookEvent)
                             .parseWebhookResponse(parseWebhookResponse)
                             .build();
    filterRequestData = FilterRequestData.builder().webhookPayloadData(webhookPayloadData).build();
    assertThat(filter.shouldEvaluateOnDelegate(filterRequestData)).isTrue();

    // Push gitlab , commits > 20
    parseWebhookResponse =
        ParseWebhookResponse.newBuilder().setPush(PushHook.newBuilder().addAllCommits(commits).build()).build();
    triggerWebhookEvent.setSourceRepoType("Gitlab");
    webhookPayloadData = WebhookPayloadData.builder()
                             .originalEvent(triggerWebhookEvent)
                             .parseWebhookResponse(parseWebhookResponse)
                             .build();
    filterRequestData = FilterRequestData.builder().webhookPayloadData(webhookPayloadData).build();
    assertThat(filter.shouldEvaluateOnDelegate(filterRequestData)).isTrue();
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGetFilesFromPushPayload() {
    TriggerWebhookEvent triggerWebhookEvent =
        TriggerWebhookEvent.builder().payload(pushPayload).sourceRepoType("Github").build();
    Set<String> filesFromPushPayload = filter.getFilesFromPushPayload(
        FilterRequestData.builder()
            .webhookPayloadData(WebhookPayloadData.builder().originalEvent(triggerWebhookEvent).build())
            .build());

    assertThat(filesFromPushPayload)
        .containsExactlyInAnyOrder("spec/manifest1.yml", "spec/manifest2.yml", "File1_Removed.txt", "File2_Removed.txt",
            "values/value1.yml", "values/value2.yml");
    triggerWebhookEvent.setSourceRepoType("GITHUB");
    assertThat(filesFromPushPayload)
        .containsExactlyInAnyOrder("spec/manifest1.yml", "spec/manifest2.yml", "File1_Removed.txt", "File2_Removed.txt",
            "values/value1.yml", "values/value2.yml");
    triggerWebhookEvent.setSourceRepoType("Gitlab");
    assertThat(filesFromPushPayload)
        .containsExactlyInAnyOrder("spec/manifest1.yml", "spec/manifest2.yml", "File1_Removed.txt", "File2_Removed.txt",
            "values/value1.yml", "values/value2.yml");
  }
}
