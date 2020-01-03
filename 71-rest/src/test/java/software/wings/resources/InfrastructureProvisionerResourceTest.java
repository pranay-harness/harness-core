package software.wings.resources;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.PROVISIONER_ID;
import static software.wings.utils.WingsTestConstants.PROVISIONER_NAME;

import io.harness.category.element.UnitTests;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import org.assertj.core.api.Assertions;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.WingsBaseTest;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.TerraformInfrastructureProvisioner;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.utils.ResourceTestRule;
import software.wings.utils.WingsTestConstants;

import javax.ws.rs.core.GenericType;

public class InfrastructureProvisionerResourceTest extends WingsBaseTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock static InfrastructureProvisionerService INFRASTRUCTURE_PROVISIONER_SERVICE;
  @InjectMocks
  private static InfrastructureProvisionerResource infrastructureProvisionerResource =
      new InfrastructureProvisionerResource();

  /**
   * The constant RESOURCES.
   */
  @ClassRule
  public static ResourceTestRule RESOURCES =
      ResourceTestRule.builder().addResource(infrastructureProvisionerResource).build();

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testGet() {
    InfrastructureProvisioner expectedProvisioner =
        TerraformInfrastructureProvisioner.builder().name(PROVISIONER_NAME).appId(APP_ID).uuid(PROVISIONER_ID).build();
    Mockito.when(INFRASTRUCTURE_PROVISIONER_SERVICE.get(WingsTestConstants.APP_ID, PROVISIONER_ID))
        .thenReturn(expectedProvisioner);
    RESOURCES.client()
        .target("/infrastructure-provisioners/" + PROVISIONER_ID + "?appId=" + WingsTestConstants.APP_ID)
        .request()
        .get(new GenericType<RestResponse<TerraformInfrastructureProvisioner>>() {});

    Assertions.assertThatThrownBy(()
                                      -> RESOURCES.client()
                                             .target("/infrastructure-provisioners/" + PROVISIONER_ID)
                                             .request()
                                             .get(new GenericType<RestResponse<InfrastructureProvisioner>>() {}));
  }
}
