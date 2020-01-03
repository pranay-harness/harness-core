package io.harness.batch.processing.writer;

import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.batch.processing.ccm.InstanceEvent;
import io.harness.batch.processing.ccm.InstanceEvent.EventType;
import io.harness.batch.processing.ccm.InstanceInfo;
import io.harness.batch.processing.ccm.InstanceType;
import io.harness.batch.processing.dao.intfc.InstanceDataDao;
import io.harness.batch.processing.integration.EcsEventGenerator;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Instant;
import java.util.Arrays;

@RunWith(MockitoJUnitRunner.class)
public class InstanceInfoLifecycleWriterTest extends CategoryTest implements EcsEventGenerator {
  @InjectMocks private InstanceEventWriter instanceEventWriter;
  @InjectMocks private InstanceInfoWriter instanceInfoWriter;
  @Mock private InstanceDataDao instanceDataDao;

  private final String TEST_INSTANCE_ID = "TEST_INSTANCE_ID_" + this.getClass().getSimpleName();
  private final String TEST_ACCOUNT_ID = "ACCOUNT_ID_" + this.getClass().getSimpleName();

  private final Instant INSTANCE_START_INSTANT = Instant.now();

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldWriteInstanceInfo() throws Exception {
    InstanceInfo instanceInfo = InstanceInfo.builder()
                                    .accountId(TEST_ACCOUNT_ID)
                                    .instanceType(InstanceType.K8S_POD)
                                    .instanceId(TEST_INSTANCE_ID)
                                    .build();
    instanceInfoWriter.write(Arrays.asList(instanceInfo));
    ArgumentCaptor<InstanceInfo> instanceDataArgumentCaptor = ArgumentCaptor.forClass(InstanceInfo.class);
    verify(instanceDataDao).upsert(instanceDataArgumentCaptor.capture());
    assertThat(instanceDataArgumentCaptor.getValue()).isEqualTo(instanceInfo);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldWriteInstanceEvent() throws Exception {
    InstanceEvent instanceEvent = InstanceEvent.builder()
                                      .accountId(TEST_ACCOUNT_ID)
                                      .type(EventType.START)
                                      .timestamp(INSTANCE_START_INSTANT)
                                      .instanceId(TEST_INSTANCE_ID)
                                      .build();
    instanceEventWriter.write(Arrays.asList(instanceEvent));
    ArgumentCaptor<InstanceEvent> instanceEventArgumentCaptor = ArgumentCaptor.forClass(InstanceEvent.class);
    verify(instanceDataDao).upsert(instanceEventArgumentCaptor.capture());
    assertThat(instanceEventArgumentCaptor.getValue()).isEqualTo(instanceEvent);
  }
}
