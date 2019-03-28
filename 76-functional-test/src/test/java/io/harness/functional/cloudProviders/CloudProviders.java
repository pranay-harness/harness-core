package io.harness.functional.cloudProviders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import io.harness.RestUtils.CloudProviderRestUtil;
import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
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
public class CloudProviders extends AbstractFunctionalTest {
  // Test Constants
  static String cloudProviderId = "";

  @Test
  @Owner(emails = "sunil@harness.io", resent = false)
  @Category(FunctionalTests.class)
  public void TC0_listCloudProviders() {
    JsonPath cloudProviders = CloudProviderRestUtil.list(bearerToken, getAccount().getUuid());
    assertThat(cloudProviders).isNotNull();
  }

  @Test
  @Owner(emails = "sunil@harness.io", resent = false)
  @Category(FunctionalTests.class)
  public void TC1_createCloudProvider() {
    JsonPath cloudProviders = CloudProviderRestUtil.list(bearerToken, getAccount().getUuid());
    assertThat(cloudProviders).isNotNull();
    int countBeforeCreate =
        CloudProviderRestUtil.verifyCloudProviders(bearerToken, getAccount().getUuid(), cloudProviders);

    SettingAttribute awsAttribute =
        aSettingAttribute()
            .withCategory(SettingCategory.CLOUD_PROVIDER)
            .withName("AWS-Automation-CloudProvider" + System.currentTimeMillis())
            .withAccountId(getAccount().getUuid())
            .withValue(AwsConfig.builder()
                           .accessKey(new ScmSecret().decryptToString(new SecretName("aws_playground_access_key")))
                           .secretKey(new ScmSecret().decryptToCharArray(new SecretName("aws_playground_secret_key")))
                           .accountId(getAccount().getUuid())
                           .build())
            .build();

    JsonPath setAttrResponse = CloudProviderRestUtil.create(bearerToken, getAccount().getUuid(), awsAttribute);
    assertThat(setAttrResponse).isNotNull();
    // System.out.println(setAttrResponse.prettyPrint());
    cloudProviderId = setAttrResponse.getString("resource.uuid");

    // After creation get all the CloudProviders including new cloud Providers.
    cloudProviders = CloudProviderRestUtil.list(bearerToken, getAccount().getUuid());
    assertThat(cloudProviders).isNotNull();
    int countAfterCreate =
        CloudProviderRestUtil.verifyCloudProviders(bearerToken, getAccount().getUuid(), cloudProviders);
    assertEquals(countBeforeCreate + 1, countAfterCreate);
  }

  @Test
  @Owner(emails = "sunil@harness.io", resent = false)
  @Category(FunctionalTests.class)
  public void TC3_deleteCloudProvider() {
    JsonPath cloudProviders = CloudProviderRestUtil.list(bearerToken, getAccount().getUuid());
    assertThat(cloudProviders).isNotNull();
    int countBeforeDelete =
        CloudProviderRestUtil.verifyCloudProviders(bearerToken, getAccount().getUuid(), cloudProviders);

    CloudProviderRestUtil.delete(bearerToken, getAccount().getUuid(), cloudProviderId);

    cloudProviders = CloudProviderRestUtil.list(bearerToken, getAccount().getUuid());
    assertThat(cloudProviders).isNotNull();
    int countAfterDelete =
        CloudProviderRestUtil.verifyCloudProviders(bearerToken, getAccount().getUuid(), cloudProviders);
    assertEquals(countBeforeDelete - 1, countAfterDelete);
  }
}