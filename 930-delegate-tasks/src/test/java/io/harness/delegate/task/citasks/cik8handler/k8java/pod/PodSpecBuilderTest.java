/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.citasks.cik8handler.k8java.pod;

import static io.harness.delegate.task.citasks.cik8handler.k8java.pod.PodSpecBuilderTestHelper.basicContainerBuilder;
import static io.harness.delegate.task.citasks.cik8handler.k8java.pod.PodSpecBuilderTestHelper.basicContainerParamsWithImageCred;
import static io.harness.delegate.task.citasks.cik8handler.k8java.pod.PodSpecBuilderTestHelper.basicContainerParamsWithoutImageCred;
import static io.harness.delegate.task.citasks.cik8handler.k8java.pod.PodSpecBuilderTestHelper.basicExpectedPod;
import static io.harness.delegate.task.citasks.cik8handler.k8java.pod.PodSpecBuilderTestHelper.basicExpectedPodWithImageCred;
import static io.harness.delegate.task.citasks.cik8handler.k8java.pod.PodSpecBuilderTestHelper.basicExpectedPodWithPVC;
import static io.harness.delegate.task.citasks.cik8handler.k8java.pod.PodSpecBuilderTestHelper.basicExpectedPodWithVolumeMount;
import static io.harness.delegate.task.citasks.cik8handler.k8java.pod.PodSpecBuilderTestHelper.basicInput;
import static io.harness.delegate.task.citasks.cik8handler.k8java.pod.PodSpecBuilderTestHelper.basicInputWithImageCred;
import static io.harness.delegate.task.citasks.cik8handler.k8java.pod.PodSpecBuilderTestHelper.basicInputWithPVC;
import static io.harness.delegate.task.citasks.cik8handler.k8java.pod.PodSpecBuilderTestHelper.basicInputWithVolumeMount;
import static io.harness.delegate.task.citasks.cik8handler.k8java.pod.PodSpecBuilderTestHelper.containerBuilderWithVolumeMount;
import static io.harness.delegate.task.citasks.cik8handler.k8java.pod.PodSpecBuilderTestHelper.containerParamsWithSecretEnvVar;
import static io.harness.delegate.task.citasks.cik8handler.k8java.pod.PodSpecBuilderTestHelper.containerParamsWithVoluemMount;
import static io.harness.delegate.task.citasks.cik8handler.k8java.pod.PodSpecBuilderTestHelper.expectedPodWithInitContainer;
import static io.harness.delegate.task.citasks.cik8handler.k8java.pod.PodSpecBuilderTestHelper.getPodSpecWithEnvSecret;
import static io.harness.rule.OwnerRule.HARSH;
import static io.harness.rule.OwnerRule.SHUBHAM;

import static junit.framework.TestCase.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ci.pod.CIK8ContainerParams;
import io.harness.delegate.beans.ci.pod.CIK8PodParams;
import io.harness.delegate.beans.ci.pod.PodParams;
import io.harness.delegate.task.citasks.cik8handler.k8java.container.ContainerSpecBuilder;
import io.harness.delegate.task.citasks.cik8handler.k8java.container.ContainerSpecBuilderResponse;
import io.harness.rule.Owner;

import io.kubernetes.client.openapi.models.V1ContainerBuilder;
import io.kubernetes.client.openapi.models.V1LocalObjectReferenceBuilder;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PodSpecBuilderTest extends CategoryTest {
  @Mock private ContainerSpecBuilder containerSpecBuilder;

  @InjectMocks private PodSpecBuilder cik8PodSpecBuilder;

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

    V1Pod expectedPod = basicExpectedPod();
    V1ContainerBuilder containerBuilder = basicContainerBuilder();

    when(containerSpecBuilder.createSpec(containerParams))
        .thenReturn(ContainerSpecBuilderResponse.builder().containerBuilder(containerBuilder).build());
    V1PodBuilder responsePodBuilder = cik8PodSpecBuilder.createSpec((PodParams) podParams);
    assertEquals(responsePodBuilder.build(), expectedPod);
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createBasicSpecWithImageCred() {
    CIK8PodParams<CIK8ContainerParams> podParams = basicInputWithImageCred();
    CIK8ContainerParams containerParams = basicContainerParamsWithImageCred();

    V1Pod expectedPod = basicExpectedPodWithImageCred();
    V1ContainerBuilder containerBuilder = basicContainerBuilder();

    when(containerSpecBuilder.createSpec(containerParams))
        .thenReturn(ContainerSpecBuilderResponse.builder()
                        .imageSecret(new V1LocalObjectReferenceBuilder().withName(registrySecretName).build())
                        .containerBuilder(containerBuilder)
                        .build());
    V1PodBuilder responsePodBuilder = cik8PodSpecBuilder.createSpec((PodParams) podParams);
    assertEquals(responsePodBuilder.build(), expectedPod);
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createBasicSpecWithVolumeMount() {
    CIK8PodParams<CIK8ContainerParams> podParams = basicInputWithVolumeMount();
    CIK8ContainerParams containerParams = containerParamsWithVoluemMount();

    V1Pod expectedPod = basicExpectedPodWithVolumeMount();
    V1ContainerBuilder containerBuilder = containerBuilderWithVolumeMount();

    when(containerSpecBuilder.createSpec(containerParams))
        .thenReturn(ContainerSpecBuilderResponse.builder()
                        .imageSecret(new V1LocalObjectReferenceBuilder().withName(registrySecretName).build())
                        .containerBuilder(containerBuilder)
                        .build());
    V1PodBuilder responsePodBuilder = cik8PodSpecBuilder.createSpec((PodParams) podParams);
    assertThat(responsePodBuilder.build()).isEqualTo(expectedPod);
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createBasicSpecWithPVC() {
    CIK8PodParams<CIK8ContainerParams> podParams = basicInputWithPVC();
    CIK8ContainerParams containerParams = containerParamsWithVoluemMount();

    V1Pod expectedPod = basicExpectedPodWithPVC();
    V1ContainerBuilder containerBuilder = containerBuilderWithVolumeMount();

    when(containerSpecBuilder.createSpec(containerParams))
        .thenReturn(ContainerSpecBuilderResponse.builder()
                        .imageSecret(new V1LocalObjectReferenceBuilder().withName(registrySecretName).build())
                        .containerBuilder(containerBuilder)
                        .build());
    V1PodBuilder responsePodBuilder = cik8PodSpecBuilder.createSpec((PodParams) podParams);
    assertThat(responsePodBuilder.build()).isEqualTo(expectedPod);
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createSpecWithInitContainer() {
    CIK8PodParams<CIK8ContainerParams> podParams = basicInput();
    CIK8ContainerParams containerParams = basicContainerParamsWithoutImageCred();

    V1Pod expectedPod = expectedPodWithInitContainer();
    V1ContainerBuilder containerBuilder = basicContainerBuilder();

    when(containerSpecBuilder.createSpec(containerParams))
        .thenReturn(ContainerSpecBuilderResponse.builder().containerBuilder(containerBuilder).build());
    V1PodBuilder responsePodBuilder = cik8PodSpecBuilder.createSpec((PodParams) podParams);
    assertEquals(responsePodBuilder.build(), expectedPod);
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createSpecWithInitContainerAndVolume() {
    CIK8PodParams<CIK8ContainerParams> podParams = basicInput();
    CIK8ContainerParams containerParams = basicContainerParamsWithoutImageCred();

    V1Pod expectedPod = PodSpecBuilderTestHelper.expectedPodWithInitContainerAndVolume();
    V1ContainerBuilder containerBuilder = PodSpecBuilderTestHelper.basicContainerBuilder();

    when(containerSpecBuilder.createSpec(containerParams))
        .thenReturn(ContainerSpecBuilderResponse.builder().containerBuilder(containerBuilder).build());
    V1PodBuilder responsePodBuilder = cik8PodSpecBuilder.createSpec((PodParams) podParams);
    assertEquals(responsePodBuilder.build(), expectedPod);
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void createSpecWithSecretEnv() {
    CIK8PodParams<CIK8ContainerParams> podParams = getPodSpecWithEnvSecret();
    CIK8ContainerParams containerParams = containerParamsWithSecretEnvVar();
    V1Pod expectedPod = basicExpectedPod();
    V1ContainerBuilder containerBuilder = basicContainerBuilder();

    when(containerSpecBuilder.createSpec(containerParams))
        .thenReturn(ContainerSpecBuilderResponse.builder().containerBuilder(containerBuilder).build());
    V1PodBuilder responsePodBuilder = cik8PodSpecBuilder.createSpec((PodParams) podParams);
    assertEquals(responsePodBuilder.build(), expectedPod);
  }
}
