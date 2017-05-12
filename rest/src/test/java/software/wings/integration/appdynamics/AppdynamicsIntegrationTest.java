package software.wings.integration.appdynamics;

import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import software.wings.beans.AppDynamicsConfig.Builder;
import software.wings.beans.RestResponse;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.integration.BaseIntegrationTest;
import software.wings.rules.Integration;
import software.wings.service.impl.appdynamics.AppdynamicsApplicationResponse;

import java.util.Arrays;
import java.util.List;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

/**
 * Created by rsingh on 5/11/17.
 */
@Integration
public class AppdynamicsIntegrationTest extends BaseIntegrationTest {
  @Before
  public void setUp() throws Exception {
    loginAdminUser();
    deleteAllDocuments(Arrays.asList(SettingAttribute.class));
    SettingAttribute appdSettingAttribute =
        aSettingAttribute()
            .withCategory(Category.CONNECTOR)
            .withName("AppDynamics")
            .withAccountId(accountId)
            .withValue(Builder.anAppDynamicsConfig()
                           .withControllerUrl("https://wings251.saas.appdynamics.com/controller")
                           .withUsername("appd-user")
                           .withAccountname("wings251")
                           .withPassword("5PdEYf9H")
                           .build())
            .build();
    wingsPersistence.saveAndGet(SettingAttribute.class, appdSettingAttribute);
  }

  @Test
  public void testGetAllApplications() throws Exception {
    final List<SettingAttribute> appdynamicsSettings =
        settingsService.getGlobalSettingAttributesByType(accountId, "APP_DYNAMICS");
    Assert.assertEquals(1, appdynamicsSettings.size());

    // get all applications
    WebTarget target = client.target(API_BASE
        + "/appdynamics/applications?settingId=" + appdynamicsSettings.get(0).getUuid() + "&accountId=" + accountId);
    RestResponse<List<AppdynamicsApplicationResponse>> restResponse = getRequestBuilderWithAuthHeader(target).get(
        new GenericType<RestResponse<List<AppdynamicsApplicationResponse>>>() {});
    Assert.assertEquals(0, restResponse.getResponseMessages().size());
    Assert.assertEquals(2, restResponse.getResource().size());
  }
}
