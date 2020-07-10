

package io.harness.stateutils.buildstate;

import static io.harness.common.CIExecutionConstants.ACCESS_KEY_MINIO_VARIABLE;
import static io.harness.common.CIExecutionConstants.BUCKET_MINIO_VARIABLE;
import static io.harness.common.CIExecutionConstants.BUCKET_MINIO_VARIABLE_VALUE;
import static io.harness.common.CIExecutionConstants.ENDPOINT_MINIO_VARIABLE;
import static io.harness.common.CIExecutionConstants.ENDPOINT_MINIO_VARIABLE_VALUE;
import static io.harness.common.CIExecutionConstants.SECRET_KEY_MINIO_VARIABLE;
import static io.harness.rule.OwnerRule.HARSH;
import static io.harness.stateutils.buildstate.providers.InternalContainerParamsProvider.ContainerKind.ADDON_CONTAINER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static software.wings.common.CICommonPodConstants.MOUNT_PATH;
import static software.wings.common.CICommonPodConstants.STEP_EXEC;

import com.google.inject.Inject;

import io.harness.beans.environment.K8BuildJobEnvInfo;
import io.harness.category.element.UnitTests;
import io.harness.executionplan.CIExecutionPlanTestHelper;
import io.harness.executionplan.CIExecutionTest;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.stateutils.buildstate.providers.InternalContainerParamsProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.ci.pod.CIContainerType;
import software.wings.beans.ci.pod.CIK8ContainerParams;
import software.wings.beans.ci.pod.CIK8PodParams;
import software.wings.beans.ci.pod.ContainerResourceParams;
import software.wings.beans.ci.pod.ImageDetailsWithConnector;
import software.wings.beans.container.ImageDetails;

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

  private static final List<String> command = Collections.unmodifiableList(Arrays.asList("/bin/sh", "-c"));
  private static final List<String> args =
      Collections.unmodifiableList(Arrays.asList("trap : TERM INT; (while true; do sleep 1000; done) & wait"));

  @Before
  public void setUp() {
    on(buildSetupUtils).set("k8BuildSetupUtils", k8BuildSetupUtils);
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldCreatePodParameters() throws IOException {
    K8BuildJobEnvInfo.PodsSetupInfo podsSetupInfo = ciExecutionPlanTestHelper.getCIPodsSetupInfo();

    // when(k8BuildSetupUtils.getPodParams(any(), any())).thenReturn(executionNode);buildSetupUtils.executeCISetupTask()
    CIK8PodParams<CIK8ContainerParams> podParams =
        k8BuildSetupUtils.getPodParams(podsSetupInfo.getPodSetupInfoList().get(0), "default", command, args, null);

    Map<String, EncryptedDataDetail> envSecretVars = new HashMap<>();
    envSecretVars.put(ACCESS_KEY_MINIO_VARIABLE, null);
    envSecretVars.put(SECRET_KEY_MINIO_VARIABLE, null);
    envSecretVars.putAll(ciExecutionPlanTestHelper.getEncryptedSecrets());

    Map<String, String> envVars = new HashMap<>();
    envVars.put(ENDPOINT_MINIO_VARIABLE, ENDPOINT_MINIO_VARIABLE_VALUE);
    envVars.put(BUCKET_MINIO_VARIABLE, BUCKET_MINIO_VARIABLE_VALUE);
    envVars.putAll(ciExecutionPlanTestHelper.getEnvVars());

    Map<String, String> map = new HashMap<>();
    map.put(STEP_EXEC, MOUNT_PATH);
    assertThat(podParams.getContainerParamsList().get(0))
        .isEqualTo(CIK8ContainerParams.builder()
                       .name(ciExecutionPlanTestHelper.getPodName())
                       .containerResourceParams(ContainerResourceParams.builder()
                                                    .resourceLimitMemoryMiB(1000)
                                                    .resourceLimitMilliCpu(1000)
                                                    .resourceRequestMemoryMiB(1000)
                                                    .resourceRequestMilliCpu(1000)
                                                    .build())
                       .containerType(CIContainerType.STEP_EXECUTOR)
                       .commands(command)
                       .encryptedSecrets(envSecretVars)
                       .envVars(envVars)
                       .args(args)
                       .imageDetailsWithConnector(ImageDetailsWithConnector.builder()
                                                      .connectorName("testConnector")
                                                      .imageDetails(imageDetails)
                                                      .build())
                       .volumeToMountPath(map)
                       .build());

    assertThat(podParams.getContainerParamsList().get(1))
        .isEqualTo(InternalContainerParamsProvider.getContainerParams(ADDON_CONTAINER));
  }
}
