package io.harness.functional.cloudProviders;

import static io.harness.rule.OwnerRule.DEEPAK;
import static io.harness.rule.OwnerRule.SUNIL;
import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rule.OwnerRule.Owner;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import io.harness.testframework.restutils.SettingsUtils;
import io.restassured.path.json.JsonPath;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import software.wings.beans.AwsConfig;
import software.wings.beans.AzureConfig;
import software.wings.beans.PhysicalDataCenterConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.settings.SettingValue.SettingVariableTypes;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CloudProviderTest extends AbstractFunctionalTest {
  // Test Constants
  private static String CONNECTOR_NAME = "%s-Automation-CloudProvider-" + System.currentTimeMillis();
  private static String CATEGORY = "CLOUD_PROVIDER";
  private static String AWS_NAMESPACE = "AWS";
  private static String GCP_NAMESPACE = "GCP";
  private static String AZURE_NAMESPACE = "Azure";
  private static String PHYSICAL_DATACENTER_NAMESPACE = "Physical-DataCenter";
  private static String MODIFIED_SUFFIX = "update";

  private static String AWSCloudProviderId;
  private static String GCPCloudProviderId;
  private static String AzureCloudProviderId;
  private static String PhysicalDataCenterCloudProviderId;

  @Test
  @Owner(emails = SUNIL, resent = false)
  @Category(FunctionalTests.class)
  public void TC0_listCloudProviders() {
    JsonPath cloudProviders = SettingsUtils.listCloudproviderConnector(bearerToken, getAccount().getUuid(), CATEGORY);
    assertThat(cloudProviders).isNotNull();
  }

  @Test
  @Owner(emails = DEEPAK, resent = false)
  @Category(FunctionalTests.class)
  public void TC1_createAzureCloudProvider() {
    String AZURE_CONNECTOR_NAME = String.format(CONNECTOR_NAME, AZURE_NAMESPACE);
    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(SettingCategory.CLOUD_PROVIDER)
            .withName(AZURE_CONNECTOR_NAME)
            .withAccountId(getAccount().getUuid())
            .withValue(AzureConfig.builder()
                           .clientId(new ScmSecret().decryptToString(new SecretName("azure_client_id")))
                           .tenantId(new ScmSecret().decryptToString(new SecretName("azure_tenant_id")))
                           .key(new ScmSecret().decryptToCharArray(new SecretName("azure_key")))
                           .accountId(getAccount().getUuid())
                           .build())
            .build();

    JsonPath setAttrResponse = SettingsUtils.create(bearerToken, getAccount().getUuid(), settingAttribute);
    assertThat(setAttrResponse).isNotNull();
    // System.out.println(setAttrResponse.prettyPrint());
    AzureCloudProviderId = setAttrResponse.getString("resource.uuid").trim();

    // Verify cloudprovider is created i.e cloudprovider with specific name exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, AZURE_CONNECTOR_NAME);
    assertTrue(connectorFound);
  }

  @Test
  @Owner(emails = SUNIL, resent = false)
  @Category(FunctionalTests.class)
  public void TC1_createAWSCloudProvider() {
    String AWS_CONNECTOR_NAME = String.format(CONNECTOR_NAME, AWS_NAMESPACE);
    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(SettingCategory.CLOUD_PROVIDER)
            .withName(AWS_CONNECTOR_NAME)
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
    AWSCloudProviderId = setAttrResponse.getString("resource.uuid").trim();

    // Verify cloudprovider is created i.e cloudprovider with specific name exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, AWS_CONNECTOR_NAME);
    assertTrue(connectorFound);
  }

  @Test
  @Owner(emails = UTKARSH, resent = false)
  @Category(FunctionalTests.class)
  public void TC1_createGCPCloudProvider() {
    String GCP_CONNECTOR_NAME = String.format(CONNECTOR_NAME, GCP_NAMESPACE);
    JsonPath setAttrResponse = SettingsUtils.createGCP(bearerToken, getAccount().getUuid(), GCP_CONNECTOR_NAME);
    assertThat(setAttrResponse).isNotNull();
    //    System.out.println(setAttrResponse.prettyPrint());
    GCPCloudProviderId = setAttrResponse.getString("resource.uuid").trim();

    // Verify cloudprovider is created i.e cloudprovider with specific name exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, GCP_CONNECTOR_NAME);
    assertTrue(connectorFound);
  }

  @Test
  @Owner(emails = DEEPAK, resent = false)
  @Category(FunctionalTests.class)
  public void TC1_createPhysicalDataCenterCloudProvider() {
    String PHYSICAL_DATACENTER_CONNECTOR_NAME = String.format(CONNECTOR_NAME, PHYSICAL_DATACENTER_NAMESPACE);
    SettingAttribute settingAttribute = aSettingAttribute()
                                            .withCategory(SettingCategory.CLOUD_PROVIDER)
                                            .withName(PHYSICAL_DATACENTER_CONNECTOR_NAME)
                                            .withAccountId(getAccount().getUuid())
                                            .withValue(PhysicalDataCenterConfig.Builder.aPhysicalDataCenterConfig()
                                                           .withType(SettingVariableTypes.PHYSICAL_DATA_CENTER.name())
                                                           .build())
                                            .build();

    JsonPath setAttrResponse = SettingsUtils.create(bearerToken, getAccount().getUuid(), settingAttribute);
    assertThat(setAttrResponse).isNotNull();

    // System.out.println(setAttrResponse.prettyPrint());
    PhysicalDataCenterCloudProviderId = setAttrResponse.getString("resource.uuid").trim();

    // Verify cloudprovider is created i.e cloudprovider with specific name exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, PHYSICAL_DATACENTER_CONNECTOR_NAME);
    assertTrue(connectorFound);
  }

  @Test
  @Owner(emails = DEEPAK, resent = false)
  @Category(FunctionalTests.class)
  public void TC2_updateAWSCloudProvider() {
    String AWS_CONNECTOR_NAME = String.format(CONNECTOR_NAME, AWS_NAMESPACE) + MODIFIED_SUFFIX;
    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(SettingCategory.CLOUD_PROVIDER)
            .withName(AWS_CONNECTOR_NAME)
            .withAccountId(getAccount().getUuid())
            .withValue(AwsConfig.builder()
                           .accessKey(new ScmSecret().decryptToString(new SecretName("aws_playground_access_key")))
                           .secretKey(new ScmSecret().decryptToCharArray(new SecretName("aws_playground_secret_key")))
                           .accountId(getAccount().getUuid())
                           .build())
            .build();

    JsonPath setAttrResponse =
        SettingsUtils.update(bearerToken, getAccount().getUuid(), settingAttribute, AWSCloudProviderId);
    assertThat(setAttrResponse).isNotNull();

    // Verify cloudprovider is updated i.e cloudprovider with specific name exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, AWS_CONNECTOR_NAME);
    assertTrue(connectorFound);
  }

  @Test
  @Owner(emails = DEEPAK, resent = false)
  @Category(FunctionalTests.class)
  public void TC2_updateAzureCloudProvider() {
    String AZURE_CONNECTOR_NAME = String.format(CONNECTOR_NAME, AZURE_NAMESPACE) + MODIFIED_SUFFIX;
    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(SettingCategory.CLOUD_PROVIDER)
            .withName(AZURE_CONNECTOR_NAME)
            .withAccountId(getAccount().getUuid())
            .withValue(AzureConfig.builder()
                           .clientId(new ScmSecret().decryptToString(new SecretName("azure_client_id")))
                           .tenantId(new ScmSecret().decryptToString(new SecretName("azure_tenant_id")))
                           .key(new ScmSecret().decryptToCharArray(new SecretName("azure_key")))
                           .accountId(getAccount().getUuid())
                           .build())
            .build();

    JsonPath setAttrResponse =
        SettingsUtils.update(bearerToken, getAccount().getUuid(), settingAttribute, AzureCloudProviderId);
    assertThat(setAttrResponse).isNotNull();

    // Verify cloudprovider is created i.e cloudprovider with specific name exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, AZURE_CONNECTOR_NAME);
    assertTrue(connectorFound);
  }

  @Test
  @Owner(emails = DEEPAK, resent = false)
  @Category(FunctionalTests.class)
  public void TC2_updatePhyscialDataCenterCloudProvider() {
    String PHYSICAL_DATACENTER_CONNECTOR_NAME =
        String.format(CONNECTOR_NAME, PHYSICAL_DATACENTER_NAMESPACE) + MODIFIED_SUFFIX;
    SettingAttribute settingAttribute = aSettingAttribute()
                                            .withCategory(SettingCategory.CLOUD_PROVIDER)
                                            .withName(PHYSICAL_DATACENTER_CONNECTOR_NAME)
                                            .withAccountId(getAccount().getUuid())
                                            .withValue(PhysicalDataCenterConfig.Builder.aPhysicalDataCenterConfig()
                                                           .withType(SettingVariableTypes.PHYSICAL_DATA_CENTER.name())
                                                           .build())
                                            .build();

    JsonPath setAttrResponse =
        SettingsUtils.update(bearerToken, getAccount().getUuid(), settingAttribute, PhysicalDataCenterCloudProviderId);
    assertThat(setAttrResponse).isNotNull();
    // System.out.println(setAttrResponse.prettyPrint());

    // Verify cloudprovider is created i.e cloudprovider with specific name exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, PHYSICAL_DATACENTER_CONNECTOR_NAME);
    assertTrue(connectorFound);
  }

  @Test
  @Owner(emails = DEEPAK, resent = false)
  @Category(FunctionalTests.class)
  public void TC2_updateGCPCloudProvider() {
    String GCP_CONNECTOR_NAME = String.format(CONNECTOR_NAME, GCP_NAMESPACE) + MODIFIED_SUFFIX;
    JsonPath setAttrResponse =
        SettingsUtils.updateGCP(bearerToken, getAccount().getUuid(), GCP_CONNECTOR_NAME, GCPCloudProviderId);
    assertThat(setAttrResponse).isNotNull();
    //    System.out.println(setAttrResponse.prettyPrint());

    // Verify cloudprovider is created i.e cloudprovider with specific name exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, GCP_CONNECTOR_NAME);
    assertTrue(connectorFound);
  }

  @Test
  @Owner(emails = SUNIL, resent = false)
  @Category(FunctionalTests.class)
  public void TC3_deleteAWSCloudProvider() {
    String AWS_CONNECTOR_NAME = String.format(CONNECTOR_NAME, AWS_NAMESPACE) + MODIFIED_SUFFIX;
    SettingsUtils.delete(bearerToken, getAccount().getUuid(), AWSCloudProviderId);

    // Verify cloudprovider is deleted i.e cloudprovider with specific name doesn't exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, AWS_CONNECTOR_NAME);
    assertFalse(connectorFound);
  }

  @Test
  @Owner(emails = UTKARSH, resent = false)
  @Category(FunctionalTests.class)
  public void TC3_deleteGCPCloudProvider() {
    String GCP_CONNECTOR_NAME = String.format(CONNECTOR_NAME, GCP_NAMESPACE) + MODIFIED_SUFFIX;
    SettingsUtils.delete(bearerToken, getAccount().getUuid(), GCPCloudProviderId);

    // Verify cloudprovider is deleted i.e cloudprovider with specific name doesn't exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, GCP_CONNECTOR_NAME);
    assertFalse(connectorFound);
  }

  @Test
  @Owner(emails = DEEPAK, resent = false)
  @Category(FunctionalTests.class)
  public void TC3_deleteAzureCloudProvider() {
    String AZURE_CONNECTOR_NAME = String.format(CONNECTOR_NAME, AZURE_NAMESPACE) + MODIFIED_SUFFIX;
    SettingsUtils.delete(bearerToken, getAccount().getUuid(), AzureCloudProviderId);

    // Verify cloudprovider is deleted i.e cloudprovider with specific name doesn't exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, AZURE_CONNECTOR_NAME);
    assertFalse(connectorFound);
  }

  @Test
  @Owner(emails = DEEPAK, resent = false)
  @Category(FunctionalTests.class)
  public void TC3_deletePhysicalDataCenterCloudProvider() {
    String PHYSICAL_DATACENTER_CONNECTOR_NAME =
        String.format(CONNECTOR_NAME, PHYSICAL_DATACENTER_NAMESPACE) + MODIFIED_SUFFIX;
    SettingsUtils.delete(bearerToken, getAccount().getUuid(), PhysicalDataCenterCloudProviderId);

    // Verify cloudprovider is deleted i.e cloudprovider with specific name doesn't exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, PHYSICAL_DATACENTER_CONNECTOR_NAME);
    assertFalse(connectorFound);
  }
}