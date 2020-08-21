

package io.harness.stateutils.buildstate;

import static io.harness.common.CIExecutionConstants.ACCESS_KEY_MINIO_VARIABLE;
import static io.harness.common.CIExecutionConstants.BUCKET_MINIO_VARIABLE;
import static io.harness.common.CIExecutionConstants.BUCKET_MINIO_VARIABLE_VALUE;
import static io.harness.common.CIExecutionConstants.DELEGATE_SERVICE_ENDPOINT_VARIABLE;
import static io.harness.common.CIExecutionConstants.DELEGATE_SERVICE_ENDPOINT_VARIABLE_VALUE;
import static io.harness.common.CIExecutionConstants.DELEGATE_SERVICE_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.DELEGATE_SERVICE_ID_VARIABLE_VALUE;
import static io.harness.common.CIExecutionConstants.DELEGATE_SERVICE_TOKEN_VARIABLE;
import static io.harness.common.CIExecutionConstants.ENDPOINT_MINIO_VARIABLE;
import static io.harness.common.CIExecutionConstants.ENDPOINT_MINIO_VARIABLE_VALUE;
import static io.harness.common.CIExecutionConstants.SECRET_KEY_MINIO_VARIABLE;
import static io.harness.rule.OwnerRule.HARSH;
import static io.harness.stateutils.buildstate.providers.InternalContainerParamsProvider.ContainerKind.ADDON_CONTAINER;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static software.wings.common.CICommonPodConstants.MOUNT_PATH;
import static software.wings.common.CICommonPodConstants.STEP_EXEC;

import com.google.inject.Inject;

import io.harness.beans.environment.K8BuildJobEnvInfo;
import io.harness.category.element.UnitTests;
import io.harness.executionplan.CIExecutionPlanTestHelper;
import io.harness.executionplan.CIExecutionTest;
import io.harness.k8s.model.ImageDetails;
import io.harness.rule.Owner;
import io.harness.stateutils.buildstate.providers.InternalContainerParamsProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.ci.pod.CIContainerType;
import software.wings.beans.ci.pod.CIK8ContainerParams;
import software.wings.beans.ci.pod.CIK8PodParams;
import software.wings.beans.ci.pod.ContainerResourceParams;
import software.wings.beans.ci.pod.ContainerSecrets;
import software.wings.beans.ci.pod.EncryptedVariableWithType;
import software.wings.beans.ci.pod.ImageDetailsWithConnector;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class K8BuildSetupUtilsTest extends CIExecutionTest {
  @Inject private BuildSetupUtils buildSetupUtils;
  @Inject private CIExecutionPlanTestHelper ciExecutionPlanTestHelper;
  @Inject private K8BuildSetupUtils k8BuildSetupUtils;
  private ImageDetails imageDetails = ImageDetails.builder().name("maven").tag("3.6.3-jdk-8").build();

  private static final String UUID = "UUID";
  private static final String NAME = "name";

  private static final List<String> command =
      Collections.unmodifiableList(Arrays.asList("/step-exec/.harness/bin/ci-lite-engine"));
  private static final List<String> args = Collections.unmodifiableList(Arrays.asList("stage", "--input",
      "CjASLgoFcnVuLTESC2J1aWxkU2NyaXB0GhgKES4vYnVpbGQtc2NyaXB0LnNoEgMQsAkKdgp0CghwYXJhbGxlbBIEbmFtZRowCgd0ZXN0LXAxEgt0ZXN0U2NyaXB0MRoYChEuL3Rlc3Qtc2NyaXB0MS5zaBIDELAJGjAKB3Rlc3QtcDISC3Rlc3RTY3JpcHQyGhgKES4vdGVzdC1zY3JpcHQyLnNoEgMQsAkK8AEK7QEKCHBhcmFsbGVsEgRuYW1lGlYKCXB1Ymxpc2gtMTJJEkcKDH4vRG9ja2VyZmlsZRICfi8aMwocdXMuZ2NyLmlvL2NpLXBsYXkvcG9ydGFsOnYwMRIRCg1nY3ItY29ubmVjdG9yEAEYBBqCAQoJcHVibGlzaC0yMnUScwoMfi9Eb2NrZXJmaWxlEgJ+LxpfCkggaHR0cHM6Ly85ODc5MjMxMzI4NzkuZGtyLmVjci5ldS13ZXN0LTEuYW1hem9uYXdzLmNvbS9jaS1wbGF5L3BvcnRhbDp2MDESEQoNZWNyLWNvbm5lY3RvchACGAU=",
      "--logpath", "/step-exec/.harness/logs/", "--tmppath", "/step-exec/.harness/tmp/", "--ports", "9001", "--debug"));

  private static final List<String> args2 = Collections.unmodifiableList(Arrays.asList(
      "server", "--port", "9001", "--logpath", "/step-exec/.harness/logs/", "--tmppath", "/step-exec/.harness/tmp/"));

  @Before
  public void setUp() {
    on(buildSetupUtils).set("k8BuildSetupUtils", k8BuildSetupUtils);
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldCreatePodParameters() throws IOException {
    K8BuildJobEnvInfo.PodsSetupInfo podsSetupInfo = ciExecutionPlanTestHelper.getCIPodsSetupInfoOnFirstPod();

    CIK8PodParams<CIK8ContainerParams> podParams =
        k8BuildSetupUtils.getPodParams(podsSetupInfo.getPodSetupInfoList().get(0), "default",
            ciExecutionPlanTestHelper.getExpectedLiteEngineTaskInfoOnFirstPod(), null, true);

    Map<String, EncryptedVariableWithType> envSecretVars = new HashMap<>();
    envSecretVars.put(ACCESS_KEY_MINIO_VARIABLE, null);
    envSecretVars.put(SECRET_KEY_MINIO_VARIABLE, null);
    envSecretVars.putAll(ciExecutionPlanTestHelper.getEncryptedSecrets());

    Map<String, String> envVars = new HashMap<>();
    envVars.put(ENDPOINT_MINIO_VARIABLE, ENDPOINT_MINIO_VARIABLE_VALUE);
    envVars.put(BUCKET_MINIO_VARIABLE, BUCKET_MINIO_VARIABLE_VALUE);
    envVars.put(DELEGATE_SERVICE_TOKEN_VARIABLE,
        podParams.getContainerParamsList().get(0).getEnvVars().get(DELEGATE_SERVICE_TOKEN_VARIABLE));
    envVars.put(DELEGATE_SERVICE_ENDPOINT_VARIABLE, DELEGATE_SERVICE_ENDPOINT_VARIABLE_VALUE);
    envVars.put(DELEGATE_SERVICE_ID_VARIABLE, DELEGATE_SERVICE_ID_VARIABLE_VALUE);
    envVars.putAll(ciExecutionPlanTestHelper.getEnvVars());

    Map<String, String> map = new HashMap<>();
    map.put(STEP_EXEC, MOUNT_PATH);
    assertThat(podParams.getContainerParamsList().get(0))
        .isEqualToIgnoringGivenFields(
            CIK8ContainerParams.builder()
                .name("build-setup1")
                .containerResourceParams(ContainerResourceParams.builder()
                                             .resourceLimitMemoryMiB(1000)
                                             .resourceLimitMilliCpu(1000)
                                             .resourceRequestMemoryMiB(1000)
                                             .resourceRequestMilliCpu(1000)
                                             .build())
                .containerType(CIContainerType.STEP_EXECUTOR)
                .commands(command)
                .containerSecrets(ContainerSecrets.builder().encryptedSecrets(envSecretVars).build())
                .envVars(envVars)
                .args(args)
                .imageDetailsWithConnector(ImageDetailsWithConnector.builder()
                                               .connectorName("testConnector")
                                               .imageDetails(imageDetails)
                                               .build())
                .volumeToMountPath(map)
                .build(),
            "envVars", "containerSecrets");
    assertThat(podParams.getContainerParamsList().get(0).getContainerSecrets().getEncryptedSecrets())
        .containsAllEntriesOf(envSecretVars);
    assertThat(podParams.getContainerParamsList().get(0).getEnvVars()).containsAllEntriesOf(envVars);

    assertThat(podParams.getContainerParamsList().get(1))
        .isEqualToIgnoringGivenFields(
            CIK8ContainerParams.builder()
                .name("build-setup2")
                .containerResourceParams(ContainerResourceParams.builder()
                                             .resourceLimitMemoryMiB(1000)
                                             .resourceLimitMilliCpu(1000)
                                             .resourceRequestMemoryMiB(1000)
                                             .resourceRequestMilliCpu(1000)
                                             .build())
                .containerType(CIContainerType.STEP_EXECUTOR)
                .commands(command)
                .containerSecrets(ContainerSecrets.builder().encryptedSecrets(envSecretVars).build())
                .envVars(envVars)
                .args(args2)
                .ports(singletonList(9001))
                .imageDetailsWithConnector(ImageDetailsWithConnector.builder()
                                               .connectorName("testConnector")
                                               .imageDetails(imageDetails)
                                               .build())
                .volumeToMountPath(map)
                .build(),
            "envVars", "containerSecrets");

    envVars.put(DELEGATE_SERVICE_TOKEN_VARIABLE,
        podParams.getContainerParamsList().get(1).getEnvVars().get(DELEGATE_SERVICE_TOKEN_VARIABLE));
    assertThat(podParams.getContainerParamsList().get(1).getContainerSecrets().getEncryptedSecrets())
        .containsAllEntriesOf(envSecretVars);
    assertThat(podParams.getContainerParamsList().get(1).getEnvVars()).containsAllEntriesOf(envVars);

    assertThat(podParams.getContainerParamsList().get(2))
        .isEqualTo(InternalContainerParamsProvider.getContainerParams(ADDON_CONTAINER)
                       .containerSecrets(ContainerSecrets.builder().build())
                       .build());
  }
}
