package io.harness.pms.sdk.core.response.publishers;

import static io.harness.pms.events.PmsEventFrameworkConstants.PIPELINE_MONITORING_ENABLED;
import static io.harness.rule.OwnerRule.SAHIL;

import static org.mockito.Mockito.verify;

import io.harness.category.element.UnitTests;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.impl.noop.NoOpProducer;
import io.harness.eventsframework.producer.Message;
import io.harness.pms.contracts.execution.events.EventErrorRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;
import io.harness.pms.contracts.execution.events.SdkResponseEventType;
import io.harness.pms.execution.utils.SdkResponseEventUtils;
import io.harness.rule.Owner;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class RedisSdkResponseEventPublisherTest {
  private static String PLAN_EXECUTION_ID = "planExecutionId";
  private static String NODE_EXECUTION_ID = "nodeExecutionId";

  RedisSdkResponseEventPublisher sdkResponseEventPublisher;
  Producer producer;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    producer = Mockito.mock(NoOpProducer.class);
    sdkResponseEventPublisher = new RedisSdkResponseEventPublisher(producer);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void publishEvent() {
    SdkResponseEventProto event = SdkResponseEventProto.newBuilder()
                                      .setSdkResponseEventType(SdkResponseEventType.ADD_EXECUTABLE_RESPONSE)
                                      .setEventErrorRequest(EventErrorRequest.newBuilder().build())
                                      .setNodeExecutionId(NODE_EXECUTION_ID)
                                      .setPlanExecutionId(PLAN_EXECUTION_ID)
                                      .build();
    Map<String, String> metadataMap = new HashMap<>();
    metadataMap.put(PIPELINE_MONITORING_ENABLED, "false");
    metadataMap.put("eventType", SdkResponseEventType.ADD_EXECUTABLE_RESPONSE.name());
    metadataMap.put("nodeExecutionId", SdkResponseEventUtils.getNodeExecutionId(event));
    metadataMap.put("planExecutionId", SdkResponseEventUtils.getPlanExecutionId(event));
    sdkResponseEventPublisher.publishEvent(event);
    verify(producer).send(Message.newBuilder().putAllMetadata(metadataMap).setData(event.toByteString()).build());
  }
}