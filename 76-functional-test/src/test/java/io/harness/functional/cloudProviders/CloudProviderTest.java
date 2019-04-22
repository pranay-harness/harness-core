package io.harness.functional.cloudProviders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.restutils.SettingsUtils;
import io.harness.rule.OwnerRule.Owner;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import io.restassured.path.json.JsonPath;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CloudProviderTest extends AbstractFunctionalTest {
  // Test Constants
  private static String CONNECTOR_NAME = "AWS-Automation-CloudProvider-" + System.currentTimeMillis();
  private static String CATEGORY = "CLOUD_PROVIDER";

  // Test Entities
  private static String cloudProviderId;

  @Test
  @Owner(emails = "sunil@harness.io", resent = false)
  @Category(FunctionalTests.class)
  public void TC0_listCloudProviders() {
    JsonPath cloudProviders = SettingsUtils.listCloudproviderConnector(bearerToken, getAccount().getUuid(), CATEGORY);
    assertThat(cloudProviders).isNotNull();
  }

  @Test
  @Owner(emails = "sunil@harness.io", resent = false)
  @Category(FunctionalTests.class)
  public void TC1_createCloudProvider() {
    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(SettingCategory.CLOUD_PROVIDER)
            .withName(CONNECTOR_NAME)
            .withAccountId(getAccount().getUuid())
            .withValue(AwsConfig.builder()
                           .accessKey(new ScmSecret().decryptToString(new SecretName("aws_playground_access_key")))
                           .secretKey(new ScmSecret().decryptToCharArray(new SecretName("aws_playground_secret_key")))
                           .accountId(getAccount().getUuid())
                           .build())
            .build();

    JsonPath setAttrResponse = SettingsUtils.create(bearerToken, getAccount().getUuid(), settingAttribute);
    assertThat(setAttrResponse).isNotNull();
    // System.out.println(setAttrResponse.prettyPrint());
    cloudProviderId = setAttrResponse.getString("resource.uuid").trim();

    // Verify cloudprovider is created i.e cloudprovider with specific name exist
    boolean connectorFound =
        SettingsUtils.checkCloudproviderConnectorExist(bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME);
    assertTrue(connectorFound);
  }

  @Test
  @Owner(emails = "sunil@harness.io", resent = false)
  @Category(FunctionalTests.class)
  public void TC2_deleteCloudProvider() {
    SettingsUtils.delete(bearerToken, getAccount().getUuid(), cloudProviderId);

    // Verify cloudprovider is deleted i.e cloudprovider with specific name doesn't exist
    boolean connectorFound =
        SettingsUtils.checkCloudproviderConnectorExist(bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME);
    assertFalse(connectorFound);
  }
}