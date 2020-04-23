package software.wings.delegatetasks.citasks.cik8handler.container;

import static io.harness.rule.OwnerRule.SHUBHAM;
import static junit.framework.TestCase.assertEquals;
import static software.wings.delegatetasks.citasks.cik8handler.container.ContainerSpecBuilderTestHelper.basicCreateSpecInput;
import static software.wings.delegatetasks.citasks.cik8handler.container.ContainerSpecBuilderTestHelper.basicCreateSpecResponse;
import static software.wings.delegatetasks.citasks.cik8handler.container.ContainerSpecBuilderTestHelper.basicCreateSpecWithEnvInput;
import static software.wings.delegatetasks.citasks.cik8handler.container.ContainerSpecBuilderTestHelper.basicCreateSpecWithEnvResponse;
import static software.wings.delegatetasks.citasks.cik8handler.container.ContainerSpecBuilderTestHelper.createSpecWithImageCredInput;
import static software.wings.delegatetasks.citasks.cik8handler.container.ContainerSpecBuilderTestHelper.createSpecWithImageCredResponse;
import static software.wings.delegatetasks.citasks.cik8handler.container.ContainerSpecBuilderTestHelper.createSpecWithResourcesCredInput;
import static software.wings.delegatetasks.citasks.cik8handler.container.ContainerSpecBuilderTestHelper.createSpecWithResourcesResponse;
import static software.wings.delegatetasks.citasks.cik8handler.container.ContainerSpecBuilderTestHelper.createSpecWithVolumeMountInput;
import static software.wings.delegatetasks.citasks.cik8handler.container.ContainerSpecBuilderTestHelper.createSpecWithVolumeMountResponse;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.beans.ci.pod.CIK8ContainerParams;

public class ContainerSpecBuilderTest extends WingsBaseTest {
  @InjectMocks private ContainerSpecBuilder containerSpecBuilder;

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createSpecBasic() {
    CIK8ContainerParams containerParams = basicCreateSpecInput();
    ContainerSpecBuilderResponse expectedResponse = basicCreateSpecResponse();

    ContainerSpecBuilderResponse response = containerSpecBuilder.createSpec(containerParams);
    assertEquals(expectedResponse.getContainerBuilder().build(), response.getContainerBuilder().build());
    assertEquals(expectedResponse.getVolumes(), response.getVolumes());
    assertEquals(expectedResponse.getImageSecret(), response.getImageSecret());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createSpecBasicWithEnv() {
    CIK8ContainerParams containerParams = basicCreateSpecWithEnvInput();
    ContainerSpecBuilderResponse expectedResponse = basicCreateSpecWithEnvResponse();

    ContainerSpecBuilderResponse response = containerSpecBuilder.createSpec(containerParams);
    assertEquals(expectedResponse.getContainerBuilder().build(), response.getContainerBuilder().build());
    assertEquals(expectedResponse.getVolumes(), response.getVolumes());
    assertEquals(expectedResponse.getImageSecret(), response.getImageSecret());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createSpecWithVolumeMount() {
    CIK8ContainerParams containerParams = createSpecWithVolumeMountInput();
    ContainerSpecBuilderResponse expectedResponse = createSpecWithVolumeMountResponse();

    ContainerSpecBuilderResponse response = containerSpecBuilder.createSpec(containerParams);
    assertEquals(expectedResponse.getContainerBuilder().build(), response.getContainerBuilder().build());
    assertEquals(expectedResponse.getVolumes(), response.getVolumes());
    assertEquals(expectedResponse.getImageSecret(), response.getImageSecret());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createSpecWithImageCred() {
    CIK8ContainerParams containerParams = createSpecWithImageCredInput();
    ContainerSpecBuilderResponse expectedResponse = createSpecWithImageCredResponse();

    ContainerSpecBuilderResponse response = containerSpecBuilder.createSpec(containerParams);
    assertEquals(expectedResponse.getContainerBuilder().build(), response.getContainerBuilder().build());
    assertEquals(expectedResponse.getVolumes(), response.getVolumes());
    assertEquals(expectedResponse.getImageSecret(), response.getImageSecret());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createSpecWithResource() {
    CIK8ContainerParams containerParams = createSpecWithResourcesCredInput();
    ContainerSpecBuilderResponse expectedResponse = createSpecWithResourcesResponse();

    ContainerSpecBuilderResponse response = containerSpecBuilder.createSpec(containerParams);
    assertEquals(expectedResponse.getContainerBuilder().build(), response.getContainerBuilder().build());
    assertEquals(expectedResponse.getVolumes(), response.getVolumes());
    assertEquals(expectedResponse.getImageSecret(), response.getImageSecret());
  }
}