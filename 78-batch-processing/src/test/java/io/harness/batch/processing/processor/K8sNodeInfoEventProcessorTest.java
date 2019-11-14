package io.harness.batch.processing.processor;

import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;

import io.harness.CategoryTest;
import io.harness.batch.processing.ccm.InstanceEvent;
import io.harness.batch.processing.ccm.InstanceInfo;
import io.harness.batch.processing.ccm.InstanceType;
import io.harness.batch.processing.writer.constants.InstanceMetaDataConstants;
import io.harness.batch.processing.writer.constants.K8sCCMConstants;
import io.harness.category.element.UnitTests;
import io.harness.event.grpc.PublishedMessage;
import io.harness.grpc.utils.HTimestamps;
import io.harness.perpetualtask.k8s.watch.NodeEvent;
import io.harness.perpetualtask.k8s.watch.NodeEvent.EventType;
import io.harness.perpetualtask.k8s.watch.NodeInfo;
import io.harness.perpetualtask.k8s.watch.Resource;
import io.harness.perpetualtask.k8s.watch.Resource.Quantity;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class K8sNodeInfoEventProcessorTest extends CategoryTest {
  @InjectMocks private K8sNodeEventProcessor k8sNodeEventProcessor;
  @InjectMocks private K8sNodeInfoProcessor k8sNodeInfoProcessor;

  private static final String NODE_UID = "node_uid";
  private static final String CPU_AMOUNT = "1";
  private static final String MEMORY_AMOUNT = "1024";
  private static final String NODE_NAME = "node_name";
  private static final String CLOUD_PROVIDER_ID = "cloud_provider_id";
  private static final String ACCOUNT_ID = "account_id";
  private final Instant NOW = Instant.now();
  private final Timestamp START_TIMESTAMP = HTimestamps.fromInstant(NOW.minus(1, ChronoUnit.DAYS));

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldCreateInstanceStartNodeEvent() throws Exception {
    PublishedMessage k8sNodeEventMessage =
        getK8sNodeEventMessage(NODE_UID, CLOUD_PROVIDER_ID, ACCOUNT_ID, EventType.EVENT_TYPE_START, START_TIMESTAMP);
    InstanceEvent instanceEvent = k8sNodeEventProcessor.process(k8sNodeEventMessage);
    assertThat(instanceEvent).isNotNull();
    assertThat(instanceEvent.getTimestamp()).isEqualTo(HTimestamps.toInstant(START_TIMESTAMP));
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldCreateInvalidInstanceNodeEvent() throws Exception {
    PublishedMessage k8sNodeEventMessage =
        getK8sNodeEventMessage(NODE_UID, CLOUD_PROVIDER_ID, ACCOUNT_ID, EventType.EVENT_TYPE_INVALID, START_TIMESTAMP);
    InstanceEvent instanceEvent = k8sNodeEventProcessor.process(k8sNodeEventMessage);
    assertThat(instanceEvent).isNotNull();
    assertThat(instanceEvent.getType()).isNull();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldCreateInstanceStopNodeEvent() throws Exception {
    PublishedMessage k8sNodeEventMessage =
        getK8sNodeEventMessage(NODE_UID, CLOUD_PROVIDER_ID, ACCOUNT_ID, EventType.EVENT_TYPE_STOP, START_TIMESTAMP);
    InstanceEvent instanceEvent = k8sNodeEventProcessor.process(k8sNodeEventMessage);
    assertThat(instanceEvent).isNotNull();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldCreateInstanceNodeInfo() throws Exception {
    Map<String, String> label = new HashMap<>();
    label.put(K8sCCMConstants.REGION, InstanceMetaDataConstants.REGION);
    label.put(K8sCCMConstants.INSTANCE_FAMILY, InstanceMetaDataConstants.INSTANCE_FAMILY);
    label.put(K8sCCMConstants.OPERATING_SYSTEM, InstanceMetaDataConstants.OPERATING_SYSTEM);
    Map<String, Quantity> requestQuantity = new HashMap<>();
    requestQuantity.put("cpu", getQuantity(CPU_AMOUNT, "M", ""));
    requestQuantity.put("memory", getQuantity(MEMORY_AMOUNT, "M", ""));
    Resource resource = Resource.newBuilder().putAllRequests(requestQuantity).build();
    PublishedMessage k8sNodeEventMessage =
        getK8sNodeInfoMessage(NODE_UID, NODE_NAME, CLOUD_PROVIDER_ID, ACCOUNT_ID, label, resource, START_TIMESTAMP);
    InstanceInfo instanceInfo = k8sNodeInfoProcessor.process(k8sNodeEventMessage);
    io.harness.batch.processing.ccm.Resource infoResource = instanceInfo.getResource();
    Map<String, String> metaData = instanceInfo.getMetaData();
    assertThat(instanceInfo).isNotNull();
    assertThat(instanceInfo.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(instanceInfo.getInstanceType()).isEqualTo(InstanceType.K8S_NODE);
    assertThat(infoResource.getCpuUnits()).isEqualTo(Double.valueOf(CPU_AMOUNT));
    assertThat(infoResource.getMemoryMb()).isEqualTo(Double.valueOf(MEMORY_AMOUNT));
    assertThat(metaData.get(InstanceMetaDataConstants.REGION)).isEqualTo(InstanceMetaDataConstants.REGION);
  }

  private Quantity getQuantity(String amount, String unit, String format) {
    return Quantity.newBuilder().setAmount(amount).setUnit(unit).setFormat(format).build();
  }

  private PublishedMessage getK8sNodeInfoMessage(String nodeUid, String nodeName, String cloudProviderId,
      String accountId, Map<String, String> label, Resource resource, Timestamp timestamp) {
    NodeInfo nodeInfo = NodeInfo.newBuilder()
                            .setNodeUid(nodeUid)
                            .setNodeName(nodeName)
                            .setCloudProviderId(cloudProviderId)
                            .setAccountId(accountId)
                            .setCreationTime(timestamp)
                            .putAllLabels(label)
                            .setAllocatableResource(resource)
                            .build();
    return getPublishedMessage(accountId, nodeInfo);
  }

  private PublishedMessage getK8sNodeEventMessage(
      String nodeUid, String cloudProviderId, String accountId, EventType eventType, Timestamp timestamp) {
    NodeEvent nodeEvent = NodeEvent.newBuilder()
                              .setNodeUid(nodeUid)
                              .setCloudProviderId(cloudProviderId)
                              .setAccountId(accountId)
                              .setType(eventType)
                              .setTimestamp(timestamp)
                              .build();
    return getPublishedMessage(accountId, nodeEvent);
  }

  private PublishedMessage getPublishedMessage(String accountId, Message message) {
    Any payload = Any.pack(message);
    return PublishedMessage.builder()
        .type(message.getClass().getName())
        .data(payload.toByteArray())
        .accountId(accountId)
        .build();
  }
}
