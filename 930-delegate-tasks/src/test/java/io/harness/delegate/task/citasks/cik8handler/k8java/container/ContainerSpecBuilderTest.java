/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.citasks.cik8handler.k8java.container;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static io.harness.rule.OwnerRule.SHUBHAM;

import static junit.framework.TestCase.assertEquals;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ci.pod.CIK8ContainerParams;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class ContainerSpecBuilderTest extends CategoryTest {
  @InjectMocks private ContainerSpecBuilder containerSpecBuilder;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createSpecBasic() {
    CIK8ContainerParams containerParams = ContainerSpecBuilderTestHelper.basicCreateSpecInput();
    ContainerSpecBuilderResponse expectedResponse = ContainerSpecBuilderTestHelper.basicCreateSpecResponse();

    ContainerSpecBuilderResponse response = containerSpecBuilder.createSpec(containerParams);
    assertEquals(expectedResponse.getContainerBuilder().build(), response.getContainerBuilder().build());
    assertEquals(expectedResponse.getVolumes(), response.getVolumes());
    assertEquals(expectedResponse.getImageSecret(), response.getImageSecret());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createSpecBasicWithEnv() {
    CIK8ContainerParams containerParams = ContainerSpecBuilderTestHelper.basicCreateSpecWithEnvInput();
    ContainerSpecBuilderResponse expectedResponse = ContainerSpecBuilderTestHelper.basicCreateSpecWithEnvResponse();

    ContainerSpecBuilderResponse response = containerSpecBuilder.createSpec(containerParams);
    assertEquals(expectedResponse.getContainerBuilder().build(), response.getContainerBuilder().build());
    assertEquals(expectedResponse.getVolumes(), response.getVolumes());
    assertEquals(expectedResponse.getImageSecret(), response.getImageSecret());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createSpecWithVolumeMount() {
    CIK8ContainerParams containerParams = ContainerSpecBuilderTestHelper.createSpecWithVolumeMountInput();
    ContainerSpecBuilderResponse expectedResponse = ContainerSpecBuilderTestHelper.createSpecWithVolumeMountResponse();

    ContainerSpecBuilderResponse response = containerSpecBuilder.createSpec(containerParams);
    assertEquals(expectedResponse.getContainerBuilder().build(), response.getContainerBuilder().build());
    assertEquals(expectedResponse.getVolumes(), response.getVolumes());
    assertEquals(expectedResponse.getImageSecret(), response.getImageSecret());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createSpecWithImageCred() {
    CIK8ContainerParams containerParams = ContainerSpecBuilderTestHelper.createSpecWithImageCredInput();
    ContainerSpecBuilderResponse expectedResponse = ContainerSpecBuilderTestHelper.createSpecWithImageCredResponse();

    ContainerSpecBuilderResponse response = containerSpecBuilder.createSpec(containerParams);
    assertEquals(expectedResponse.getContainerBuilder().build(), response.getContainerBuilder().build());
    assertEquals(expectedResponse.getVolumes(), response.getVolumes());
    assertEquals(expectedResponse.getImageSecret(), response.getImageSecret());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createSpecWithResource() {
    CIK8ContainerParams containerParams = ContainerSpecBuilderTestHelper.createSpecWithResourcesCredInput();
    ContainerSpecBuilderResponse expectedResponse = ContainerSpecBuilderTestHelper.createSpecWithResourcesResponse();

    ContainerSpecBuilderResponse response = containerSpecBuilder.createSpec(containerParams);
    assertEquals(expectedResponse.getContainerBuilder().build(), response.getContainerBuilder().build());
    assertEquals(expectedResponse.getVolumes(), response.getVolumes());
    assertEquals(expectedResponse.getImageSecret(), response.getImageSecret());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createSpecWithPortWorkingDirSecretEnv() {
    CIK8ContainerParams containerParams = ContainerSpecBuilderTestHelper.basicCreateSpecWithSecretEnvPortWorkingDir();
    ContainerSpecBuilderResponse expectedResponse =
        ContainerSpecBuilderTestHelper.basicCreateSpecWithSecretEnvPortWorkingDirResponse();

    ContainerSpecBuilderResponse response = containerSpecBuilder.createSpec(containerParams);
    assertEquals(expectedResponse.getContainerBuilder().build(), response.getContainerBuilder().build());
    assertEquals(expectedResponse.getVolumes(), response.getVolumes());
    assertEquals(expectedResponse.getImageSecret(), response.getImageSecret());
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void createSpecWithSecretVolume() {
    CIK8ContainerParams containerParams = ContainerSpecBuilderTestHelper.createSpecWithSecretVolumes();
    ContainerSpecBuilderResponse expectedResponse =
        ContainerSpecBuilderTestHelper.createSpecWithSecretVolumesResponse();

    ContainerSpecBuilderResponse response = containerSpecBuilder.createSpec(containerParams);
    assertEquals(expectedResponse.getContainerBuilder().build(), response.getContainerBuilder().build());
    assertEquals(expectedResponse.getVolumes(), response.getVolumes());
    assertEquals(expectedResponse.getImageSecret(), response.getImageSecret());
  }
}
