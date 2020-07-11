package io.harness.ng.core.remote.client.rest.factory;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.VIKAS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.ManagerDelegateServiceDriver;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ResponseData;
import io.harness.ng.core.BaseTest;
import io.harness.ng.core.remote.NGDelegateClientResource;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class NGDelegateClientResourceTest extends BaseTest {
  @Mock ManagerDelegateServiceDriver managerDelegateServiceDriver;
  private NGDelegateClientResource ngDelegateClientResource;

  @Before
  public void doSetup() {
    ngDelegateClientResource = new NGDelegateClientResource(managerDelegateServiceDriver);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testCreate() {
    final ResponseData responseData = new ResponseData() {};
    when(managerDelegateServiceDriver.sendTask(any(String.class), any(), any())).thenReturn(responseData);
    final ResponseData response = ngDelegateClientResource.create(generateUuid());
    assertThat(response).isNotNull().isEqualTo(responseData);
  }
}
