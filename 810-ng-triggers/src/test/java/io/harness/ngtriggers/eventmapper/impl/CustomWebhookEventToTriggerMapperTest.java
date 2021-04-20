package io.harness.ngtriggers.eventmapper.impl;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.ROHITKARELIA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.HeaderConfig;
import io.harness.category.element.UnitTests;
import io.harness.ngtriggers.beans.config.NGTriggerConfig;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventMappingResponse;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.source.NGTriggerSource;
import io.harness.ngtriggers.beans.source.webhook.CustomWebhookTriggerSpec;
import io.harness.ngtriggers.beans.source.webhook.WebhookTriggerConfig;
import io.harness.ngtriggers.mapper.NGTriggerElementMapper;
import io.harness.ngtriggers.service.NGTriggerService;
import io.harness.rule.Owner;

import com.google.common.io.Resources;
import com.google.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(PIPELINE)
public class CustomWebhookEventToTriggerMapperTest extends CategoryTest {
  private String ngTriggerCustomYaml;
  @InjectMocks @Inject CustomWebhookEventToTriggerMapper customWebhookEventToTriggerMapper;
  @Mock private NGTriggerService ngTriggerService;
  @Mock private NGTriggerElementMapper ngTriggerElementMapper;

  @Before
  public void setup() throws IOException {
    MockitoAnnotations.initMocks(this);
    ClassLoader classLoader = getClass().getClassLoader();
    String filename = "ng-custom-trigger.yaml";
    ngTriggerCustomYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(filename)), StandardCharsets.UTF_8);
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testMapCustomWebhookEventToTriggers() {
    TriggerWebhookEvent event =
        TriggerWebhookEvent.builder()
            .accountId("accountId")
            .createdAt(1l)
            .headers(Arrays.asList(
                HeaderConfig.builder().key("content-type").values(Arrays.asList("application/json")).build()))
            .build();
    NGTriggerEntity ngTriggerEntity = NGTriggerEntity.builder().yaml(ngTriggerCustomYaml).build();
    List<NGTriggerEntity> ngTriggerEntityList = new ArrayList<>();
    ngTriggerEntityList.add(ngTriggerEntity);

    NGTriggerConfig ngTriggerConfig =
        NGTriggerConfig.builder()
            .source(NGTriggerSource.builder()
                        .spec(WebhookTriggerConfig.builder().spec(CustomWebhookTriggerSpec.builder().build()).build())
                        .build())
            .build();

    when(ngTriggerService.findTriggersForCustomWehbook(event, false, true)).thenReturn(ngTriggerEntityList);
    doReturn(ngTriggerConfig).when(ngTriggerElementMapper).toTriggerConfig(ngTriggerCustomYaml);
    WebhookEventMappingResponse webhookEventMappingResponse =
        customWebhookEventToTriggerMapper.mapWebhookEventToTriggers(event);
    assertThat(webhookEventMappingResponse).isNotNull();
    assertThat(webhookEventMappingResponse.getTriggers().size()).isEqualTo(1);
    assertThat(webhookEventMappingResponse.isCustomTrigger()).isTrue();
  }
}
