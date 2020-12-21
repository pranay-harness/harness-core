package io.harness.ngtriggers.mapper;

import static io.harness.ngtriggers.beans.source.NGTriggerType.WEBHOOK;
import static io.harness.ngtriggers.beans.source.webhook.WebhookAction.CLOSED;
import static io.harness.ngtriggers.beans.source.webhook.WebhookAction.OPENED;
import static io.harness.ngtriggers.beans.source.webhook.WebhookEvent.PULL_REQUEST;
import static io.harness.ngtriggers.beans.target.TargetType.PIPELINE;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.ngtriggers.beans.config.NGTriggerConfig;
import io.harness.ngtriggers.beans.dto.NGTriggerResponseDTO;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.TriggerEventHistory;
import io.harness.ngtriggers.beans.entity.metadata.NGTriggerMetadata;
import io.harness.ngtriggers.beans.source.webhook.WebhookTriggerConfig;
import io.harness.ngtriggers.beans.source.webhook.WebhookTriggerSpec;
import io.harness.repositories.ng.core.spring.TriggerEventHistoryRepository;
import io.harness.rule.Owner;

import com.google.common.io.Resources;
import com.google.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class NGTriggerElementMapperTest extends CategoryTest {
  private String ngTriggerYaml;
  @Mock private TriggerEventHistoryRepository triggerEventHistoryRepository;
  @InjectMocks @Inject private NGTriggerElementMapper ngTriggerElementMapper;

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
    ClassLoader classLoader = getClass().getClassLoader();
    String filename = "ng-trigger.yaml";
    ngTriggerYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(filename)), StandardCharsets.UTF_8);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testToTriggerConfig() {
    NGTriggerConfig trigger = ngTriggerElementMapper.toTriggerConfig(ngTriggerYaml);
    assertThat(trigger).isNotNull();
    assertThat(trigger.getIdentifier()).isEqualTo("first_trigger");
    assertThat(trigger.getSource().getType()).isEqualTo(WEBHOOK);
    assertThat(trigger.getSource().getSpec()).isInstanceOfAny(WebhookTriggerConfig.class);

    WebhookTriggerConfig webhookTriggerConfig = (WebhookTriggerConfig) trigger.getSource().getSpec();
    assertThat(webhookTriggerConfig.getType()).isEqualTo("Github");
    assertThat(webhookTriggerConfig.getSpec()).isNotNull();

    WebhookTriggerSpec webhookTriggerConfigSpec = webhookTriggerConfig.getSpec();
    assertThat(webhookTriggerConfigSpec.getEvent()).isEqualTo(PULL_REQUEST);
    assertThat(webhookTriggerConfigSpec.getActions()).containsExactlyInAnyOrder(OPENED, CLOSED);
    assertThat(webhookTriggerConfigSpec.getRepoUrl()).isEqualTo("https://github.com/test/myrepo");
    assertThat(webhookTriggerConfigSpec.getPathFilters()).containsExactlyInAnyOrder("path1", "path2");
    assertThat(webhookTriggerConfigSpec.getPayloadConditions()).isNotNull();
    assertThat(webhookTriggerConfigSpec.getPayloadConditions().size()).isEqualTo(3);

    Set<String> payloadConditionSet = webhookTriggerConfigSpec.getPayloadConditions()
                                          .stream()
                                          .map(webhookPayloadCondition
                                              -> new StringBuilder(128)
                                                     .append(webhookPayloadCondition.getKey())
                                                     .append(':')
                                                     .append(webhookPayloadCondition.getOperator())
                                                     .append(':')
                                                     .append(webhookPayloadCondition.getValue())
                                                     .toString())
                                          .collect(Collectors.toSet());

    assertThat(payloadConditionSet)
        .containsOnly("sourceBranch:equals:dev", "targetBranch:in:master, on-prem",
            "${pull_request.number}:regex:^pr-[0-9a-f]{7}$");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testToTriggerEntityFromYaml() {
    NGTriggerEntity ngTriggerEntity =
        ngTriggerElementMapper.toTriggerDetails("accId", "orgId", "projId", ngTriggerYaml).getNgTriggerEntity();

    assertThat(ngTriggerEntity.getAccountId()).isEqualTo("accId");
    assertThat(ngTriggerEntity.getOrgIdentifier()).isEqualTo("orgId");
    assertThat(ngTriggerEntity.getProjectIdentifier()).isEqualTo("projId");
    assertThat(ngTriggerEntity.getYaml()).isEqualTo(ngTriggerYaml);
    assertThat(ngTriggerEntity.getIdentifier()).isEqualTo("first_trigger");
    assertThat(ngTriggerEntity.getName()).isEqualTo("first trigger");
    assertThat(ngTriggerEntity.getTargetType()).isEqualTo(PIPELINE);
    assertThat(ngTriggerEntity.getTargetIdentifier()).isEqualTo("myPipeline");

    NGTriggerMetadata metadata = ngTriggerEntity.getMetadata();
    assertThat(metadata).isNotNull();
    assertThat(metadata.getWebhook()).isNotNull();
    assertThat(metadata.getWebhook().getRepoURL()).isEqualTo("https://github.com/test/myrepo");
    assertThat(metadata.getWebhook().getType()).isEqualTo("Github");
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testToTriggerEntityWithWrongIdentifier() {
    assertThatThrownBy(
        () -> ngTriggerElementMapper.toTriggerEntity("accId", "orgId", "projId", "not_first_trigger", ngTriggerYaml))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testPrepareExecutionDataArray() throws Exception {
    String sDate0 = "23-Dec-1998 02:37:50";
    String sDate1 = "24-Dec-1998 02:37:50";
    String sDate2 = "24-Dec-1998 12:37:50";
    String sDate3 = "25-Dec-1998 14:37:50";
    String sDate4 = "25-Dec-1998 15:37:50";
    String sDate5 = "25-Dec-1998 16:37:50";
    String sDate6 = "25-Dec-1998 20:37:50";
    String sDate7 = "26-Dec-1998 01:37:50";
    String sDate8 = "26-Dec-1998 11:12:50";
    String sDate9 = "26-Dec-1998 21:37:50";
    String sDate10 = "26-Dec-1998 22:37:50";
    String sDate11 = "26-Dec-1998 23:37:50";
    String sDate12 = "26-Dec-1998 23:47:50";
    String sDate13 = "27-Dec-1998 02:37:50";
    String sDate14 = "27-Dec-1998 21:37:50";
    String sDate15 = "29-Dec-1998 23:37:50";
    String sDate16 = "29-Dec-1998 13:37:50";
    String sDate17 = "29-Dec-1998 14:37:50";
    String sDate18 = "29-Dec-1998 15:37:50";
    String sDate19 = "29-Dec-1998 16:37:50";
    String sDate20 = "30-Dec-1998 17:37:50";
    String sDate21 = "30-Dec-1998 18:37:50";

    SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
    List<TriggerEventHistory> triggerEventHistories = Arrays.asList(
        generateEventHistoryWithTimestamp(formatter, sDate0), generateEventHistoryWithTimestamp(formatter, sDate1),
        generateEventHistoryWithTimestamp(formatter, sDate2), generateEventHistoryWithTimestamp(formatter, sDate3),
        generateEventHistoryWithTimestamp(formatter, sDate4), generateEventHistoryWithTimestamp(formatter, sDate5),
        generateEventHistoryWithTimestamp(formatter, sDate6), generateEventHistoryWithTimestamp(formatter, sDate7),
        generateEventHistoryWithTimestamp(formatter, sDate8), generateEventHistoryWithTimestamp(formatter, sDate9),
        generateEventHistoryWithTimestamp(formatter, sDate10), generateEventHistoryWithTimestamp(formatter, sDate11),
        generateEventHistoryWithTimestamp(formatter, sDate12), generateEventHistoryWithTimestamp(formatter, sDate13),
        generateEventHistoryWithTimestamp(formatter, sDate14), generateEventHistoryWithTimestamp(formatter, sDate15),
        generateEventHistoryWithTimestamp(formatter, sDate16), generateEventHistoryWithTimestamp(formatter, sDate17),
        generateEventHistoryWithTimestamp(formatter, sDate18), generateEventHistoryWithTimestamp(formatter, sDate19),
        generateEventHistoryWithTimestamp(formatter, sDate20), generateEventHistoryWithTimestamp(formatter, sDate21));

    Integer[] executionData = ngTriggerElementMapper.prepareExecutionDataArray(
        formatter.parse("30-Dec-1998 21:37:50").getTime(), triggerEventHistories);
    assertThat(executionData).containsExactlyInAnyOrder(2, 5, 0, 2, 6, 4, 2);
  }

  private TriggerEventHistory generateEventHistoryWithTimestamp(SimpleDateFormat formatter6, String sDate1)
      throws ParseException {
    return TriggerEventHistory.builder().createdAt(formatter6.parse(sDate1).getTime()).build();
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testToResponseDTO() {
    NGTriggerEntity ngTriggerEntity =
        ngTriggerElementMapper.toTriggerDetails("accId", "orgId", "projId", ngTriggerYaml).getNgTriggerEntity();
    NGTriggerResponseDTO responseDTO = ngTriggerElementMapper.toResponseDTO(ngTriggerEntity);
    assertThat(responseDTO.getAccountIdentifier()).isEqualTo(ngTriggerEntity.getAccountId());
    assertThat(responseDTO.getOrgIdentifier()).isEqualTo(ngTriggerEntity.getOrgIdentifier());
    assertThat(responseDTO.getProjectIdentifier()).isEqualTo(ngTriggerEntity.getProjectIdentifier());
    assertThat(responseDTO.getTargetIdentifier()).isEqualTo(ngTriggerEntity.getTargetIdentifier());
    assertThat(responseDTO.getYaml()).isEqualTo(ngTriggerEntity.getYaml());
    assertThat(responseDTO.getType()).isEqualTo(ngTriggerEntity.getType());
    assertThat(responseDTO.getIdentifier()).isEqualTo(ngTriggerEntity.getIdentifier());
    assertThat(responseDTO.getName()).isEqualTo(ngTriggerEntity.getName());
    assertThat(responseDTO.getDescription()).isEqualTo(ngTriggerEntity.getDescription());
    assertThat(responseDTO.getDescription()).isEqualTo(ngTriggerEntity.getDescription());
  }
}
