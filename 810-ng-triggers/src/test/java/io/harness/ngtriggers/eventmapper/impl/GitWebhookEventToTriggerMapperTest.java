/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ngtriggers.eventmapper.impl;

import static io.harness.ngtriggers.beans.response.TriggerEventResponse.FinalStatus.SCM_SERVICE_CONNECTION_FAILED;
import static io.harness.rule.OwnerRule.ADWAIT;

import static io.grpc.Status.UNAVAILABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ngtriggers.beans.dto.TriggerMappingRequestData;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventMappingResponse;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.helpers.TriggerFilterStore;
import io.harness.ngtriggers.mapper.NGTriggerElementMapper;
import io.harness.ngtriggers.service.NGTriggerService;
import io.harness.ngtriggers.utils.WebhookEventPayloadParser;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import io.grpc.StatusRuntimeException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class GitWebhookEventToTriggerMapperTest extends CategoryTest {
  @Mock WebhookEventPayloadParser webhookEventPayloadParser;
  @Mock NGTriggerElementMapper ngTriggerElementMapper;
  @Mock NGTriggerService ngTriggerService;
  @Inject TriggerFilterStore triggerFilterStore;
  @InjectMocks @Inject GitWebhookEventToTriggerMapper mapper;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testParseEventData() {
    TriggerWebhookEvent event = TriggerWebhookEvent.builder().createdAt(1l).build();
    StatusRuntimeException statusRuntimeException = new StatusRuntimeException(UNAVAILABLE);
    doThrow(statusRuntimeException).when(webhookEventPayloadParser).parseEvent(event);

    WebhookEventMappingResponse webhookEventMappingResponse =
        mapper.mapWebhookEventToTriggers(TriggerMappingRequestData.builder().triggerWebhookEvent(event).build());
    assertThat(webhookEventMappingResponse.isFailedToFindTrigger()).isTrue();
    assertThat(webhookEventMappingResponse.getWebhookEventResponse().isExceptionOccurred()).isTrue();
    assertThat(webhookEventMappingResponse.getWebhookEventResponse().getFinalStatus())
        .isEqualTo(SCM_SERVICE_CONNECTION_FAILED);
  }
}
