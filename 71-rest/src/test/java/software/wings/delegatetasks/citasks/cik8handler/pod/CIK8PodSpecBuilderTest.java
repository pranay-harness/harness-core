package software.wings.delegatetasks.citasks.cik8handler.pod;

import static io.harness.rule.OwnerRule.SHUBHAM;
import static junit.framework.TestCase.assertEquals;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static software.wings.delegatetasks.citasks.cik8handler.pod.CIK8PodSpecBuilderTestHelper.basicContainerBuilder;
import static software.wings.delegatetasks.citasks.cik8handler.pod.CIK8PodSpecBuilderTestHelper.basicContainerParamsWithImageCred;
import static software.wings.delegatetasks.citasks.cik8handler.pod.CIK8PodSpecBuilderTestHelper.basicContainerParamsWithoutImageCred;
import static software.wings.delegatetasks.citasks.cik8handler.pod.CIK8PodSpecBuilderTestHelper.basicExpectedPod;
import static software.wings.delegatetasks.citasks.cik8handler.pod.CIK8PodSpecBuilderTestHelper.basicExpectedPodWithImageCred;
import static software.wings.delegatetasks.citasks.cik8handler.pod.CIK8PodSpecBuilderTestHelper.basicExpectedPodWithVolumeMount;
import static software.wings.delegatetasks.citasks.cik8handler.pod.CIK8PodSpecBuilderTestHelper.basicInput;
import static software.wings.delegatetasks.citasks.cik8handler.pod.CIK8PodSpecBuilderTestHelper.basicInputWithImageCred;
import static software.wings.delegatetasks.citasks.cik8handler.pod.CIK8PodSpecBuilderTestHelper.basicInputWithVolumeMount;
import static software.wings.delegatetasks.citasks.cik8handler.pod.CIK8PodSpecBuilderTestHelper.containerBuilderWithVolumeMount;
import static software.wings.delegatetasks.citasks.cik8handler.pod.CIK8PodSpecBuilderTestHelper.containerParamsWithVoluemMount;
import static software.wings.delegatetasks.citasks.cik8handler.pod.CIK8PodSpecBuilderTestHelper.expectedPodWithInitContainer;
import static software.wings.delegatetasks.citasks.cik8handler.pod.CIK8PodSpecBuilderTestHelper.expectedPodWithInitContainerAndVolume;
import static software.wings.delegatetasks.citasks.cik8handler.pod.CIK8PodSpecBuilderTestHelper.gitCloneCtrBuilder;

import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.ci.pod.CIK8ContainerParams;
import software.wings.beans.ci.pod.CIK8PodParams;
import software.wings.beans.ci.pod.PodParams;
import software.wings.delegatetasks.citasks.cik8handler.container.ContainerSpecBuilder;
import software.wings.delegatetasks.citasks.cik8handler.container.ContainerSpecBuilderResponse;
import software.wings.delegatetasks.citasks.cik8handler.container.GitCloneContainerSpecBuilder;

import java.util.Arrays;

public class CIK8PodSpecBuilderTest extends WingsBaseTest {
  @Mock private GitCloneContainerSpecBuilder gitCloneContainerSpecBuilder;
  @Mock private ContainerSpecBuilder containerSpecBuilder;

  @InjectMocks private CIK8PodSpecBuilder cik8PodSpecBuilder;

  private String registrySecretName = "hs-index-docker-io-v1-usr-hs";

  @Before
  public void setUp() {
    on(cik8PodSpecBuilder).set("containerSpecBuilder", containerSpecBuilder);
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createBasicSpec() {
    CIK8PodParams<CIK8ContainerParams> podParams = basicInput();
    CIK8ContainerParams containerParams = basicContainerParamsWithoutImageCred();

    Pod expectedPod = basicExpectedPod();
    ContainerBuilder containerBuilder = basicContainerBuilder();

    when(containerSpecBuilder.createSpec(containerParams))
        .thenReturn(ContainerSpecBuilderResponse.builder().containerBuilder(containerBuilder).build());
    when(gitCloneContainerSpecBuilder.createGitCloneSpec(any())).thenReturn(null);
    PodBuilder responsePodBuilder = cik8PodSpecBuilder.createSpec((PodParams) podParams);
    assertEquals(responsePodBuilder.build(), expectedPod);
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createBasicSpecWithImageCred() {
    CIK8PodParams<CIK8ContainerParams> podParams = basicInputWithImageCred();
    CIK8ContainerParams containerParams = basicContainerParamsWithImageCred();

    Pod expectedPod = basicExpectedPodWithImageCred();
    ContainerBuilder containerBuilder = basicContainerBuilder();

    when(containerSpecBuilder.createSpec(containerParams))
        .thenReturn(ContainerSpecBuilderResponse.builder()
                        .imageSecret(new LocalObjectReference(registrySecretName))
                        .containerBuilder(containerBuilder)
                        .build());
    when(gitCloneContainerSpecBuilder.createGitCloneSpec(any())).thenReturn(null);
    PodBuilder responsePodBuilder = cik8PodSpecBuilder.createSpec((PodParams) podParams);
    assertEquals(responsePodBuilder.build(), expectedPod);
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createBasicSpecWithVolumeMount() {
    CIK8PodParams<CIK8ContainerParams> podParams = basicInputWithVolumeMount();
    CIK8ContainerParams containerParams = containerParamsWithVoluemMount();

    Pod expectedPod = basicExpectedPodWithVolumeMount();
    ContainerBuilder containerBuilder = containerBuilderWithVolumeMount();

    when(containerSpecBuilder.createSpec(containerParams))
        .thenReturn(ContainerSpecBuilderResponse.builder()
                        .imageSecret(new LocalObjectReference(registrySecretName))
                        .containerBuilder(containerBuilder)
                        .build());
    when(gitCloneContainerSpecBuilder.createGitCloneSpec(any())).thenReturn(null);
    PodBuilder responsePodBuilder = cik8PodSpecBuilder.createSpec((PodParams) podParams);
    assertEquals(responsePodBuilder.build(), expectedPod);
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createSpecWithInitContainer() {
    CIK8PodParams<CIK8ContainerParams> podParams = basicInput();
    CIK8ContainerParams containerParams = basicContainerParamsWithoutImageCred();

    Pod expectedPod = expectedPodWithInitContainer();
    ContainerBuilder gitCloneCtrBuilder = gitCloneCtrBuilder();
    ContainerBuilder containerBuilder = basicContainerBuilder();

    when(containerSpecBuilder.createSpec(containerParams))
        .thenReturn(ContainerSpecBuilderResponse.builder().containerBuilder(containerBuilder).build());
    when(gitCloneContainerSpecBuilder.createGitCloneSpec(any()))
        .thenReturn(ContainerSpecBuilderResponse.builder().containerBuilder(gitCloneCtrBuilder).build());
    PodBuilder responsePodBuilder = cik8PodSpecBuilder.createSpec((PodParams) podParams);
    assertEquals(responsePodBuilder.build(), expectedPod);
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createSpecWithInitContainerAndVolume() {
    CIK8PodParams<CIK8ContainerParams> podParams = basicInput();
    CIK8ContainerParams containerParams = basicContainerParamsWithoutImageCred();

    Pod expectedPod = expectedPodWithInitContainerAndVolume();
    ContainerBuilder gitCloneCtrBuilder = gitCloneCtrBuilder();
    ContainerBuilder containerBuilder = basicContainerBuilder();

    when(containerSpecBuilder.createSpec(containerParams))
        .thenReturn(ContainerSpecBuilderResponse.builder().containerBuilder(containerBuilder).build());
    when(gitCloneContainerSpecBuilder.createGitCloneSpec(any()))
        .thenReturn(ContainerSpecBuilderResponse.builder()
                        .containerBuilder(gitCloneCtrBuilder)
                        .volumes(Arrays.asList(new Volume()))
                        .build());
    PodBuilder responsePodBuilder = cik8PodSpecBuilder.createSpec((PodParams) podParams);
    assertEquals(responsePodBuilder.build(), expectedPod);
  }
}