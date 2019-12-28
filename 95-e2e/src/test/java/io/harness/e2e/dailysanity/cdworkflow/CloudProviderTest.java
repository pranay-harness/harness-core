package io.harness.e2e.dailysanity.cdworkflow;

import static io.harness.rule.OwnerRule.NATARAJA;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import io.harness.category.element.E2ETests;
import io.harness.e2e.AbstractE2ETest;
import io.harness.rule.OwnerRule.Owner;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import io.harness.testframework.restutils.SettingsUtils;
import io.restassured.path.json.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;

@Slf4j
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CloudProviderTest extends AbstractE2ETest {
  // Test Constants
  private static String CONNECTOR_NAME = "AWS-Automation-CloudProvider-" + System.currentTimeMillis();
  private static String CATEGORY = "CLOUD_PROVIDER";

  // Test Entities
  private static String cloudProviderId;

  @Test
  @Owner(developers = NATARAJA)
  @Category(E2ETests.class)
  public void TC0_listCloudProviders() {
    JsonPath cloudProviders = SettingsUtils.listCloudproviderConnector(bearerToken, getAccount().getUuid(), CATEGORY);
    assertThat(cloudProviders).isNotNull();
  }

  @Test
  @Owner(developers = NATARAJA)
  @Category(E2ETests.class)
  public void TC1_createCloudProvider() {
    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(SettingCategory.CLOUD_PROVIDER)
            .withName(CONNECTOR_NAME)
            .withAccountId(getAccount().getUuid())
            .withValue(
                AwsConfig.builder()
                    .accessKey(new ScmSecret().decryptToString(new SecretName("qe_aws_playground_access_key")))
                    .secretKey(new ScmSecret().decryptToCharArray(new SecretName("qe_aws_playground_secret_key")))
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
    assertThat(connectorFound).isTrue();
  }

  @Test
  @Owner(developers = NATARAJA)
  @Category(E2ETests.class)
  public void TC2_deleteCloudProvider() {
    SettingsUtils.delete(bearerToken, getAccount().getUuid(), cloudProviderId);

    // Verify cloudprovider is deleted i.e cloudprovider with specific name doesn't exist
    boolean connectorFound =
        SettingsUtils.checkCloudproviderConnectorExist(bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME);
    assertThat(connectorFound).isFalse();
  }
}