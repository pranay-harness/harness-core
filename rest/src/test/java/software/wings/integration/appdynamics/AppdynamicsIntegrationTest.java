package software.wings.integration.appdynamics;

import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import software.wings.beans.AppDynamicsConfig.Builder;
import software.wings.beans.RestResponse;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.integration.BaseIntegrationTest;
import software.wings.service.impl.appdynamics.AppdynamicsApplication;
import software.wings.service.impl.appdynamics.AppdynamicsBusinessTransaction;
import software.wings.service.impl.appdynamics.AppdynamicsNode;
import software.wings.service.impl.appdynamics.AppdynamicsTier;

import java.util.Arrays;
import java.util.List;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

/**
 * Created by rsingh on 5/11/17.
 */
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
    RestResponse<List<AppdynamicsApplication>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<AppdynamicsApplication>>>() {});

    Assert.assertEquals(0, restResponse.getResponseMessages().size());
    Assert.assertTrue(restResponse.getResource().size() > 0);

    for (AppdynamicsApplication app : restResponse.getResource()) {
      Assert.assertTrue(app.getId() > 0);
      Assert.assertFalse(StringUtils.isBlank(app.getName()));
    }
  }

  @Test
  public void testGetAllTiers() throws Exception {
    final List<SettingAttribute> appdynamicsSettings =
        settingsService.getGlobalSettingAttributesByType(accountId, "APP_DYNAMICS");
    Assert.assertEquals(1, appdynamicsSettings.size());

    // get all applications
    WebTarget target = client.target(API_BASE
        + "/appdynamics/applications?settingId=" + appdynamicsSettings.get(0).getUuid() + "&accountId=" + accountId);
    RestResponse<List<AppdynamicsApplication>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<AppdynamicsApplication>>>() {});

    long appId = 0;

    for (AppdynamicsApplication application : restResponse.getResource()) {
      if (application.getName().equalsIgnoreCase("MyApp")) {
        appId = application.getId();
        break;
      }
    }

    Assert.assertTrue("could not find MyApp application in appdynamics", appId > 0);
    WebTarget btTarget = client.target(API_BASE + "/appdynamics/tiers?settingId=" + appdynamicsSettings.get(0).getUuid()
        + "&accountId=" + accountId + "&appdynamicsAppId=" + appId);
    RestResponse<List<AppdynamicsTier>> tierRestResponse =
        getRequestBuilderWithAuthHeader(btTarget).get(new GenericType<RestResponse<List<AppdynamicsTier>>>() {});
    Assert.assertTrue(tierRestResponse.getResource().size() > 0);

    for (AppdynamicsTier tier : tierRestResponse.getResource()) {
      Assert.assertTrue(tier.getId() > 0);
      Assert.assertFalse(StringUtils.isBlank(tier.getName()));
      Assert.assertFalse(StringUtils.isBlank(tier.getType()));
      Assert.assertFalse(StringUtils.isBlank(tier.getAgentType()));
      Assert.assertTrue(tier.getNumberOfNodes() > 0);
    }
  }

  @Test
  public void testGetAllNodes() throws Exception {
    final List<SettingAttribute> appdynamicsSettings =
        settingsService.getGlobalSettingAttributesByType(accountId, "APP_DYNAMICS");
    Assert.assertEquals(1, appdynamicsSettings.size());

    // get all applications
    WebTarget target = client.target(API_BASE
        + "/appdynamics/applications?settingId=" + appdynamicsSettings.get(0).getUuid() + "&accountId=" + accountId);
    RestResponse<List<AppdynamicsApplication>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<AppdynamicsApplication>>>() {});

    long appId = 0;

    for (AppdynamicsApplication application : restResponse.getResource()) {
      if (application.getName().equalsIgnoreCase("MyApp")) {
        appId = application.getId();
        break;
      }
    }

    Assert.assertTrue("could not find MyApp application in appdynamics", appId > 0);
    WebTarget btTarget = client.target(API_BASE + "/appdynamics/tiers?settingId=" + appdynamicsSettings.get(0).getUuid()
        + "&accountId=" + accountId + "&appdynamicsAppId=" + appId);
    RestResponse<List<AppdynamicsTier>> tierRestResponse =
        getRequestBuilderWithAuthHeader(btTarget).get(new GenericType<RestResponse<List<AppdynamicsTier>>>() {});
    Assert.assertTrue(tierRestResponse.getResource().size() > 0);

    for (AppdynamicsTier tier : tierRestResponse.getResource()) {
      Assert.assertTrue(tier.getId() > 0);
      Assert.assertFalse(StringUtils.isBlank(tier.getName()));
      Assert.assertFalse(StringUtils.isBlank(tier.getType()));
      Assert.assertFalse(StringUtils.isBlank(tier.getAgentType()));
      Assert.assertTrue(tier.getNumberOfNodes() > 0);

      WebTarget nodeTarget =
          client.target(API_BASE + "/appdynamics/nodes?settingId=" + appdynamicsSettings.get(0).getUuid()
              + "&accountId=" + accountId + "&appdynamicsAppId=" + appId + "&tierId=" + tier.getId());

      RestResponse<List<AppdynamicsNode>> nodeRestResponse =
          getRequestBuilderWithAuthHeader(nodeTarget).get(new GenericType<RestResponse<List<AppdynamicsNode>>>() {});

      Assert.assertTrue(nodeRestResponse.getResource().size() > 0);
      for (AppdynamicsNode node : nodeRestResponse.getResource()) {
        Assert.assertTrue(node.getId() > 0);
        Assert.assertFalse(StringUtils.isBlank(node.getName()));
        Assert.assertFalse(StringUtils.isBlank(node.getType()));
        Assert.assertTrue(node.getTierId() > 0);
        Assert.assertFalse(StringUtils.isBlank(node.getTierName()));
        Assert.assertTrue(node.getMachineId() > 0);
        Assert.assertFalse(StringUtils.isBlank(node.getMachineName()));
        Assert.assertFalse(StringUtils.isBlank(node.getMachineOSType()));
        Assert.assertFalse(StringUtils.isBlank(node.getAppAgentVersion()));
        Assert.assertFalse(StringUtils.isBlank(node.getAgentType()));
        Assert.assertTrue(node.getIpAddresses().size() > 0);

        Assert.assertTrue(node.getIpAddresses().containsKey("ipAddresses"));
        Assert.assertTrue(node.getIpAddresses().get("ipAddresses").size() > 0);
      }
    }
  }

  @Test
  public void testGetAllBusinessTransactions() throws Exception {
    final List<SettingAttribute> appdynamicsSettings =
        settingsService.getGlobalSettingAttributesByType(accountId, "APP_DYNAMICS");
    Assert.assertEquals(1, appdynamicsSettings.size());

    // get all applications
    WebTarget target = client.target(API_BASE
        + "/appdynamics/applications?settingId=" + appdynamicsSettings.get(0).getUuid() + "&accountId=" + accountId);
    RestResponse<List<AppdynamicsApplication>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<AppdynamicsApplication>>>() {});

    long appId = 0;

    for (AppdynamicsApplication application : restResponse.getResource()) {
      if (application.getName().equalsIgnoreCase("MyApp")) {
        appId = application.getId();
        break;
      }
    }

    Assert.assertTrue("could not find MyApp application in appdynamics", appId > 0);
    WebTarget btTarget = client.target(API_BASE + "/appdynamics/business-transactions?settingId="
        + appdynamicsSettings.get(0).getUuid() + "&accountId=" + accountId + "&appdynamicsAppId=" + appId);
    RestResponse<List<AppdynamicsBusinessTransaction>> btRestResponse = getRequestBuilderWithAuthHeader(btTarget).get(
        new GenericType<RestResponse<List<AppdynamicsBusinessTransaction>>>() {});
    Assert.assertTrue(btRestResponse.getResource().size() > 0);

    for (AppdynamicsBusinessTransaction bt : btRestResponse.getResource()) {
      Assert.assertTrue(bt.getId() > 0);
      Assert.assertTrue(bt.getTierId() > 0);
      Assert.assertFalse(StringUtils.isBlank(bt.getName()));
      Assert.assertFalse(StringUtils.isBlank(bt.getEntryPointType()));
      Assert.assertFalse(StringUtils.isBlank(bt.getInternalName()));
      Assert.assertFalse(StringUtils.isBlank(bt.getTierName()));
      Assert.assertFalse(StringUtils.isBlank(bt.getInternalName()));
    }
  }
}
