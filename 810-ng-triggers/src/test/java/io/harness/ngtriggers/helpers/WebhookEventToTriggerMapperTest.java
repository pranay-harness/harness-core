package io.harness.ngtriggers.helpers;

import static io.harness.rule.OwnerRule.ADWAIT;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ngtriggers.beans.config.NGTriggerConfig;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.scm.PRWebhookEvent;
import io.harness.ngtriggers.beans.scm.ParsePayloadResponse;
import io.harness.ngtriggers.beans.scm.WebhookPayloadData;
import io.harness.ngtriggers.beans.source.NGTriggerSource;
import io.harness.ngtriggers.beans.source.NGTriggerSpec;
import io.harness.ngtriggers.beans.source.webhook.WebhookEvent;
import io.harness.ngtriggers.beans.source.webhook.WebhookTriggerConfig;
import io.harness.ngtriggers.beans.source.webhook.WebhookTriggerSpec;
import io.harness.ngtriggers.mapper.NGTriggerElementMapper;
import io.harness.ngtriggers.service.NGTriggerService;
import io.harness.ngtriggers.utils.WebhookEventPayloadParser;
import io.harness.product.ci.scm.proto.ParseWebhookResponse;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class WebhookEventToTriggerMapperTest extends CategoryTest {
  @Mock WebhookEventPayloadParser webhookEventPayloadParser;
  @Mock NGTriggerElementMapper ngTriggerElementMapper;
  @Mock NGTriggerService ngTriggerService;
  @InjectMocks @Inject WebhookEventToTriggerMapper webhookEventToTriggerMapper;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testParseEventData() {
    TriggerWebhookEvent event = TriggerWebhookEvent.builder().build();
    RuntimeException runtimeException = new RuntimeException("fake");
    doThrow(runtimeException).when(webhookEventPayloadParser).parseEvent(event);

    ParsePayloadResponse parsePayloadResponse =
        webhookEventToTriggerMapper.convertWebhookResponse(event, ParseWebhookResponse.newBuilder().build());
    assertThat(parsePayloadResponse.isExceptionOccured()).isTrue();
    assertThat(parsePayloadResponse.getException()).isEqualTo(runtimeException);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testApplyFilters() {
    NGTriggerConfig ngTriggerConfig1 =
        NGTriggerConfig.builder()
            .source(NGTriggerSource.builder()
                        .spec(WebhookTriggerConfig.builder()
                                  .spec(WebhookTriggerSpec.builder().event(WebhookEvent.PULL_REQUEST).build())
                                  .build())
                        .build())
            .build();
    NGTriggerConfig ngTriggerConfig2 =
        NGTriggerConfig.builder()
            .source(NGTriggerSource.builder()
                        .spec(WebhookTriggerConfig.builder()
                                  .spec(WebhookTriggerSpec.builder().event(WebhookEvent.MERGE_REQUEST).build())
                                  .build())
                        .build())
            .build();

    NGTriggerConfig ngTriggerConfig3 = NGTriggerConfig.builder()
                                           .source(NGTriggerSource.builder()
                                                       .spec(new NGTriggerSpec() {
                                                         @Override
                                                         public boolean equals(Object obj) {
                                                           return super.equals(obj);
                                                         }
                                                       })
                                                       .build())
                                           .build();

    doReturn(ngTriggerConfig1)
        .doReturn(ngTriggerConfig2)
        .doReturn(ngTriggerConfig3)
        .when(ngTriggerElementMapper)
        .toTriggerConfig("yaml");

    WebhookPayloadData webhookPayloadData =
        WebhookPayloadData.builder().webhookEvent(PRWebhookEvent.builder().build()).build();

    NGTriggerEntity ngTriggerEntity = NGTriggerEntity.builder().yaml("yaml").build();
    List<TriggerDetails> triggerDetails = webhookEventToTriggerMapper.applyFilters(
        webhookPayloadData, Arrays.asList(ngTriggerEntity, ngTriggerEntity, ngTriggerEntity));

    assertThat(triggerDetails.size()).isEqualTo(2);
    List<NGTriggerConfig> ngTriggerConfigs =
        triggerDetails.stream().map(TriggerDetails::getNgTriggerConfig).collect(toList());
    assertThat(ngTriggerConfigs).containsExactly(ngTriggerConfig1, ngTriggerConfig2);
  }
}
