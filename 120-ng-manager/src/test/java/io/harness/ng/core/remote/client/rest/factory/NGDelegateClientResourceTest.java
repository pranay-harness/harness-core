package io.harness.ng.core.remote.client.rest.factory;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ROHIT_KUMAR;
import static io.harness.rule.OwnerRule.VIKAS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.ManagerDelegateServiceDriver;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.ng.core.perpetualtask.sample.SampleRemotePTaskManager;
import io.harness.ng.core.remote.NGDelegateClientResource;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class NGDelegateClientResourceTest extends CategoryTest {
  @Mock ManagerDelegateServiceDriver managerDelegateServiceDriver;
  private NGDelegateClientResource ngDelegateClientResource;

  @Before
  public void doSetup() {
    MockitoAnnotations.initMocks(this);
    SampleRemotePTaskManager sampleRemotePTaskManager = new SampleRemotePTaskManager(managerDelegateServiceDriver);
    ngDelegateClientResource = new NGDelegateClientResource(managerDelegateServiceDriver, sampleRemotePTaskManager);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testCreate() {
    final DelegateResponseData responseData = new DelegateResponseData() {};
    when(managerDelegateServiceDriver.sendTask(any(String.class), any(), any())).thenReturn(responseData);
    final DelegateResponseData response = ngDelegateClientResource.create(generateUuid());
    assertThat(response).isNotNull().isEqualTo(responseData);
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void createSamplePTask() {
    doReturn("taskId")
        .when(managerDelegateServiceDriver)
        .createRemotePerpetualTask(anyString(), any(), any(), any(), anyBoolean());
    final String taskId = ngDelegateClientResource.createSamplePTask("accountId", "dummy_country", 100).getResource();
    assertThat(taskId).isEqualTo("taskId");
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void deleteSamplePTask() {
    doReturn(true).when(managerDelegateServiceDriver).deleteRemotePerpetualTask(anyString(), anyString());
    final Boolean result = ngDelegateClientResource.deleteSamplePTask("accountId", "taskId").getResource();
    assertThat(result).isEqualTo(true);
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void updateSamplePTask() {
    doReturn(true).when(managerDelegateServiceDriver).resetRemotePerpetualTask("accountId", "taskId");
    final Boolean result =
        ngDelegateClientResource.updateSamplePTask("accountId", "taskId", "dummy_country", 100).getResource();
    assertThat(result).isEqualTo(true);
  }
}
