package io.harness.delegate.task.citasks.cik8handler.pod;

import static io.harness.delegate.task.citasks.cik8handler.pod.CIK8PodSpecBuilderTestHelper.basicContainerBuilder;
import static io.harness.delegate.task.citasks.cik8handler.pod.CIK8PodSpecBuilderTestHelper.basicContainerParamsWithImageCred;
import static io.harness.delegate.task.citasks.cik8handler.pod.CIK8PodSpecBuilderTestHelper.basicContainerParamsWithoutImageCred;
import static io.harness.delegate.task.citasks.cik8handler.pod.CIK8PodSpecBuilderTestHelper.basicExpectedPod;
import static io.harness.delegate.task.citasks.cik8handler.pod.CIK8PodSpecBuilderTestHelper.basicExpectedPodWithImageCred;
import static io.harness.delegate.task.citasks.cik8handler.pod.CIK8PodSpecBuilderTestHelper.basicExpectedPodWithPVC;
import static io.harness.delegate.task.citasks.cik8handler.pod.CIK8PodSpecBuilderTestHelper.basicExpectedPodWithVolumeMount;
import static io.harness.delegate.task.citasks.cik8handler.pod.CIK8PodSpecBuilderTestHelper.basicInput;
import static io.harness.delegate.task.citasks.cik8handler.pod.CIK8PodSpecBuilderTestHelper.basicInputWithImageCred;
import static io.harness.delegate.task.citasks.cik8handler.pod.CIK8PodSpecBuilderTestHelper.basicInputWithPVC;
import static io.harness.delegate.task.citasks.cik8handler.pod.CIK8PodSpecBuilderTestHelper.basicInputWithVolumeMount;
import static io.harness.delegate.task.citasks.cik8handler.pod.CIK8PodSpecBuilderTestHelper.containerBuilderWithVolumeMount;
import static io.harness.delegate.task.citasks.cik8handler.pod.CIK8PodSpecBuilderTestHelper.containerParamsWithSecretEnvVar;
import static io.harness.delegate.task.citasks.cik8handler.pod.CIK8PodSpecBuilderTestHelper.containerParamsWithVoluemMount;
import static io.harness.delegate.task.citasks.cik8handler.pod.CIK8PodSpecBuilderTestHelper.expectedPodWithInitContainer;
import static io.harness.delegate.task.citasks.cik8handler.pod.CIK8PodSpecBuilderTestHelper.getPodSpecWithEnvSecret;
import static io.harness.delegate.task.citasks.cik8handler.pod.CIK8PodSpecBuilderTestHelper.gitCloneCtrBuilder;
import static io.harness.rule.OwnerRule.HARSH;
import static io.harness.rule.OwnerRule.SHUBHAM;

import static junit.framework.TestCase.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ci.pod.CIK8ContainerParams;
import io.harness.delegate.beans.ci.pod.CIK8PodParams;
import io.harness.delegate.beans.ci.pod.PodParams;
import io.harness.delegate.task.citasks.cik8handler.container.ContainerSpecBuilder;
import io.harness.delegate.task.citasks.cik8handler.container.ContainerSpecBuilderResponse;
import io.harness.delegate.task.citasks.cik8handler.container.GitCloneContainerSpecBuilder;
import io.harness.rule.Owner;

import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CIK8PodSpecBuilderTest extends CategoryTest {
  @Mock private GitCloneContainerSpecBuilder gitCloneContainerSpecBuilder;
  @Mock private ContainerSpecBuilder containerSpecBuilder;

  @InjectMocks private CIK8PodSpecBuilder cik8PodSpecBuilder;

  private String registrySecretName = "hs-index-docker-io-v1-usr-hs";

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
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
    assertThat(responsePodBuilder.build()).isEqualTo(expectedPod);
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createBasicSpecWithPVC() {
    CIK8PodParams<CIK8ContainerParams> podParams = basicInputWithPVC();
    CIK8ContainerParams containerParams = containerParamsWithVoluemMount();

    Pod expectedPod = basicExpectedPodWithPVC();
    ContainerBuilder containerBuilder = containerBuilderWithVolumeMount();

    when(containerSpecBuilder.createSpec(containerParams))
        .thenReturn(ContainerSpecBuilderResponse.builder()
                        .imageSecret(new LocalObjectReference(registrySecretName))
                        .containerBuilder(containerBuilder)
                        .build());
    when(gitCloneContainerSpecBuilder.createGitCloneSpec(any())).thenReturn(null);
    PodBuilder responsePodBuilder = cik8PodSpecBuilder.createSpec((PodParams) podParams);
    assertThat(responsePodBuilder.build()).isEqualTo(expectedPod);
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

    Pod expectedPod = CIK8PodSpecBuilderTestHelper.expectedPodWithInitContainerAndVolume();
    ContainerBuilder gitCloneCtrBuilder = gitCloneCtrBuilder();
    ContainerBuilder containerBuilder = CIK8PodSpecBuilderTestHelper.basicContainerBuilder();

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

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void createSpecWithSecretEnv() {
    CIK8PodParams<CIK8ContainerParams> podParams = getPodSpecWithEnvSecret();
    CIK8ContainerParams containerParams = containerParamsWithSecretEnvVar();
    Pod expectedPod = basicExpectedPod();
    ContainerBuilder containerBuilder = basicContainerBuilder();

    when(containerSpecBuilder.createSpec(containerParams))
        .thenReturn(ContainerSpecBuilderResponse.builder().containerBuilder(containerBuilder).build());
    when(gitCloneContainerSpecBuilder.createGitCloneSpec(any())).thenReturn(null);
    PodBuilder responsePodBuilder = cik8PodSpecBuilder.createSpec((PodParams) podParams);
    assertEquals(responsePodBuilder.build(), expectedPod);
  }
}
