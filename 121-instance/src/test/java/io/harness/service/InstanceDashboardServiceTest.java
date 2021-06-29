package io.harness.service;

import static io.harness.rule.OwnerRule.JASMEET;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.InstancesTestBase;
import io.harness.category.element.UnitTests;
import io.harness.entities.ArtifactDetails;
import io.harness.entities.instance.Instance;
import io.harness.models.BuildsByEnvironment;
import io.harness.models.EnvBuildInstanceCount;
import io.harness.models.InstancesByBuildId;
import io.harness.repositories.instance.InstanceRepository;
import io.harness.rule.Owner;
import io.harness.service.instancedashboardservice.InstanceDashboardService;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class InstanceDashboardServiceTest extends InstancesTestBase {
  @Inject private InstanceDashboardService instanceDashboardService;
  @Inject @Mock private InstanceRepository instanceRepository;
  private static final String ACCOUNT_IDENTIFIER = "ACCOUNT_IDENTIFIER";
  private static final String ORG_IDENTIFIER = "ORG_IDENTIFIER";
  private static final String PROJECT_IDENTIFIER = "PROJECT_IDENTIFIER";
  private static final String SERVICE_IDENTIFIER = "SERVICE_IDENTIFIER";

  private Instance createDummyInstance(String envId, String tag) {
    return Instance.builder()
        .accountIdentifier(ACCOUNT_IDENTIFIER)
        .orgIdentifier(ORG_IDENTIFIER)
        .projectIdentifier(PROJECT_IDENTIFIER)
        .serviceId(SERVICE_IDENTIFIER)
        .envId(envId)
        .envName("envName")
        .primaryArtifact(ArtifactDetails.builder().tag(tag).build())
        .createdAt(0L)
        .deletedAt(10L)
        .build();
  }

  @Test
  @Owner(developers = JASMEET)
  @Category(UnitTests.class)
  public void getActiveInstancesGroupedByEnvironmentAndBuild() {
    long currentTimestampInMs = 5L;
    for (int i = 0; i < 40; i++) {
      instanceRepository.save(createDummyInstance(String.valueOf(i % 5), String.valueOf((i / 5) % 4)));
    }

    List<BuildsByEnvironment> environments =
        instanceDashboardService.getActiveInstancesByServiceIdGroupedByEnvironmentAndBuild(
            ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, SERVICE_IDENTIFIER, currentTimestampInMs);

    assertThat(environments.size()).isEqualTo(5);
    for (BuildsByEnvironment buildsByEnv : environments) {
      List<InstancesByBuildId> instanceByBuilds = buildsByEnv.getBuilds();
      assertThat(instanceByBuilds.size()).isEqualTo(4);
      for (InstancesByBuildId instanceByBuild : instanceByBuilds) {
        assertThat(instanceByBuild.getInstances().size()).isEqualTo(2);
      }
    }
  }

  @Test
  @Owner(developers = JASMEET)
  @Category(UnitTests.class)
  public void getUniqueEnvIdBuildIdCombinationsWithInstanceCount() {
    Map<String, Map<String, Integer>> mock = new HashMap<>();
    List<Pair<Pair<String, String>, Integer>> mockList = Arrays.asList(Pair.of(Pair.of("envId1", "buildId1"), 3),
        Pair.of(Pair.of("envId1", "buildId2"), 2), Pair.of(Pair.of("envId2", "buildId1"), 2),
        Pair.of(Pair.of("envId2", "buildId2"), 5), Pair.of(Pair.of("envId3", "buildId3"), 3));

    mockList.forEach(mockListItem -> {
      final String envId = mockListItem.getLeft().getLeft();
      final String buildId = mockListItem.getLeft().getRight();
      final Integer count = mockListItem.getRight();
      if (!mock.containsKey(envId)) {
        mock.put(envId, new HashMap<>());
      }
      mock.get(envId).put(buildId, count);
      for (int i = 0; i < count; i++) {
        instanceRepository.save(createDummyInstance(envId, buildId));
      }
    });

    List<EnvBuildInstanceCount> uniqueEnvIdBuildIdCombinationsWithInstanceCounts =
        instanceDashboardService.getEnvBuildInstanceCountByServiceId(
            ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, SERVICE_IDENTIFIER, 5);
    assertThat(uniqueEnvIdBuildIdCombinationsWithInstanceCounts.size()).isGreaterThan(0);
    uniqueEnvIdBuildIdCombinationsWithInstanceCounts.forEach(uniqueEnvIdBuildIdCombinationsWithInstanceCount -> {
      final String envId = uniqueEnvIdBuildIdCombinationsWithInstanceCount.getEnvId();
      final String buildId = uniqueEnvIdBuildIdCombinationsWithInstanceCount.getTag();
      final int count = uniqueEnvIdBuildIdCombinationsWithInstanceCount.getCount();
      final int expectedCount = mock.getOrDefault(envId, new HashMap<>()).getOrDefault(buildId, 0);
      assertThat(count).isEqualTo(expectedCount);
    });
  }

  @Test
  @Owner(developers = JASMEET)
  @Category(UnitTests.class)
  public void getInstancesByEnvIdAndBuildIds() {
    Map<String, Map<String, Integer>> mock = new HashMap<>();
    List<Pair<Pair<String, String>, Integer>> mockList =
        Arrays.asList(Pair.of(Pair.of("envId1", "buildId1"), 30), Pair.of(Pair.of("envId1", "buildId2"), 30),
            Pair.of(Pair.of("envId2", "buildId1"), 30), Pair.of(Pair.of("envId2", "buildId2"), 30));

    mockList.forEach(mockListItem -> {
      final String envId = mockListItem.getLeft().getLeft();
      final String buildId = mockListItem.getLeft().getRight();
      final Integer count = mockListItem.getRight();
      if (!mock.containsKey(envId)) {
        mock.put(envId, new HashMap<>());
      }
      mock.get(envId).put(buildId, count);
      for (int i = 0; i < count; i++) {
        instanceRepository.save(createDummyInstance(envId, buildId));
      }
    });

    String inputEnvName = "envId1";
    List<String> inputBuildIds = Arrays.asList("buildId1", "buildId2");
    List<InstancesByBuildId> result;

    // Invalid cases that should return no results

    result = instanceDashboardService.getActiveInstancesByServiceIdEnvIdAndBuildIds(
        ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, "randomServiceId", inputEnvName, inputBuildIds, 5);
    assertThat(result.size()).isEqualTo(0);

    result = instanceDashboardService.getActiveInstancesByServiceIdEnvIdAndBuildIds(
        ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, SERVICE_IDENTIFIER, "randomEnvName", inputBuildIds, 5);
    assertThat(result.size()).isEqualTo(0);

    result = instanceDashboardService.getActiveInstancesByServiceIdEnvIdAndBuildIds(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER,
        PROJECT_IDENTIFIER, SERVICE_IDENTIFIER, inputEnvName, Arrays.asList("randomBuildId"), 5);
    assertThat(result.size()).isEqualTo(0);

    // Valid case

    result = instanceDashboardService.getActiveInstancesByServiceIdEnvIdAndBuildIds(
        ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, SERVICE_IDENTIFIER, inputEnvName, inputBuildIds, 5);
    assertThat(result.size()).isEqualTo(2);

    for (int i = 0; i < inputBuildIds.size(); i++) {
      assertThat(result.get(i).getBuildId()).isEqualTo(inputBuildIds.get(i));
      assertThat(result.get(i).getInstances().size()).isEqualTo(20);
    }
  }
}
