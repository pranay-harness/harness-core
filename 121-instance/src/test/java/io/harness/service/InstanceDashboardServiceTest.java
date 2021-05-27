package io.harness.service;

import static io.harness.rule.OwnerRule.JASMEET;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import io.harness.InstancesTestBase;
import io.harness.category.element.UnitTests;
import io.harness.entities.ArtifactDetails;
import io.harness.entities.instance.Instance;
import io.harness.models.BuildsByEnvironment;
import io.harness.models.InstancesByBuild;
import io.harness.repositories.instance.InstanceRepository;
import io.harness.rule.Owner;
import io.harness.service.instancedashboardservice.InstanceDashboardService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class InstanceDashboardServiceTest extends InstancesTestBase {
  @Inject private InstanceDashboardService instanceDashboardService;
  @Inject @Mock private InstanceRepository instanceRepository;
  private static final String ACCOUNT_ID = "ACCOUNT_ID";
  private static final String ORG_ID = "ORG_ID";
  private static final String PROJECT_ID = "PROJECT_ID";

  @Test
  @Owner(developers = JASMEET)
  @Category(UnitTests.class)
  public void getActiveInstancesGroupedByEnvironmentAndBuild() {
    List<Instance> instanceListMock = new ArrayList<>();
    long currentTimestampInMs = 1621900807817L;
    for (int i = 0; i < 40; i++) {
      Instance instance = Instance.builder()
                              .accountIdentifier(ACCOUNT_ID)
                              .orgIdentifier(ORG_ID)
                              .projectIdentifier(PROJECT_ID)
                              .envId(String.valueOf(i % 5))
                              .primaryArtifact(ArtifactDetails.builder().tag(String.valueOf((i / 5) % 4)).build())
                              .build();
      instanceListMock.add(instance);
    }

    doReturn(instanceListMock)
        .when(instanceRepository)
        .getActiveInstances(ACCOUNT_ID, ORG_ID, PROJECT_ID, currentTimestampInMs);

    List<BuildsByEnvironment> environments = instanceDashboardService.getActiveInstancesGroupedByEnvironmentAndBuild(
        ACCOUNT_ID, ORG_ID, PROJECT_ID, currentTimestampInMs);

    assertThat(environments.size()).isEqualTo(5);
    for (BuildsByEnvironment buildsByEnv : environments) {
      List<InstancesByBuild> instanceByBuilds = buildsByEnv.getBuilds();
      assertThat(instanceByBuilds.size()).isEqualTo(4);
      for (InstancesByBuild instanceByBuild : instanceByBuilds) {
        assertThat(instanceByBuild.getInstances().size()).isEqualTo(2);
      }
    }
  }
}
