package io.harness.batch.processing.integration.service;

import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.batch.processing.ccm.InstanceState;
import io.harness.batch.processing.ccm.InstanceType;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.entities.InstanceData.InstanceDataKeys;
import io.harness.batch.processing.service.intfc.InstanceDataService;
import io.harness.category.element.UnitTests;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import lombok.val;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
public class InstanceDataIntegrationTest extends CategoryTest {
  private final String TEST_ACCOUNT_ID = "TEST_ACCOUNT_ID_" + this.getClass().getSimpleName();
  private final String TEST_INSTANCE_ID = "TEST_INSTANCE_ID_" + this.getClass().getSimpleName();
  private final String TEST_CLUSTER_ARN = "TEST_CLUSTER_ARN_" + this.getClass().getSimpleName();

  @Autowired private InstanceDataService instanceDataService;

  @Autowired private HPersistence hPersistence;

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldCreateInstanceData() {
    InstanceData instanceData = createInstanceData(TEST_INSTANCE_ID);
    boolean instanceCreated = instanceDataService.create(instanceData);
    assertThat(instanceCreated).isTrue();

    List<InstanceState> activeInstanceStates =
        new ArrayList<>(Arrays.asList(InstanceState.INITIALIZING, InstanceState.RUNNING));
    InstanceData savedInstanceData =
        instanceDataService.fetchActiveInstanceData(TEST_ACCOUNT_ID, TEST_INSTANCE_ID, activeInstanceStates);

    Instant startTime = Instant.now().minus(1, ChronoUnit.DAYS);
    boolean instanceUpdated =
        instanceDataService.updateInstanceState(savedInstanceData, startTime, InstanceState.RUNNING);
    assertThat(instanceUpdated).isTrue();

    savedInstanceData =
        instanceDataService.fetchActiveInstanceData(TEST_ACCOUNT_ID, TEST_INSTANCE_ID, activeInstanceStates);
    assertThat(savedInstanceData.getUsageStartTime()).isEqualTo(startTime);
    assertThat(savedInstanceData.getInstanceState()).isEqualTo(InstanceState.RUNNING);

    Instant endTime = Instant.now();
    boolean instanceStopped =
        instanceDataService.updateInstanceState(savedInstanceData, endTime, InstanceState.STOPPED);
    assertThat(instanceStopped).isTrue();
  }

  private InstanceData createInstanceData(String instanceId) {
    return InstanceData.builder()
        .accountId(TEST_ACCOUNT_ID)
        .instanceId(instanceId)
        .clusterName(TEST_CLUSTER_ARN)
        .instanceType(InstanceType.EC2_INSTANCE)
        .instanceState(InstanceState.INITIALIZING)
        .build();
  }

  @After
  public void clearCollection() {
    val ds = hPersistence.getDatastore(InstanceData.class);
    ds.delete(ds.createQuery(InstanceData.class).filter(InstanceDataKeys.accountId, TEST_ACCOUNT_ID));
  }
}
