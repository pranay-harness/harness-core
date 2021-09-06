package io.harness.pms.event.webhookevent;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.DELETE_ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ENTITY_TYPE;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.PIPELINE_ENTITY;
import static io.harness.rule.OwnerRule.BRIJESH;

import static junit.framework.TestCase.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.webhookpayloads.webhookdata.WebhookDTO;
import io.harness.pms.triggers.webhook.service.TriggerWebhookExecutionServiceV2;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(PIPELINE)
public class WebhookEventStreamListenerTest extends CategoryTest {
  @Mock TriggerWebhookExecutionServiceV2 triggerWebhookExecutionServiceV2;
  @InjectMocks WebhookEventStreamListener webhookEventStreamListener;
  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void testHandleMessage() {
    Message message = Message.newBuilder().build();
    assertTrue(webhookEventStreamListener.handleMessage(message));
    message = Message.newBuilder()
                  .setMessage(io.harness.eventsframework.producer.Message.newBuilder()
                                  .putMetadata(ENTITY_TYPE, PIPELINE_ENTITY)
                                  .putMetadata(ACTION, DELETE_ACTION)
                                  .setData(WebhookDTO.newBuilder().build().toByteString())
                                  .build())
                  .build();
    webhookEventStreamListener.handleMessage(message);
    verify(triggerWebhookExecutionServiceV2, times(1)).processEvent(any());
  }
}
