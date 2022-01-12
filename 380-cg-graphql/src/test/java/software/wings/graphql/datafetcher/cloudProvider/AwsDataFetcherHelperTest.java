/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.cloudProvider;

import static io.harness.rule.OwnerRule.BOJANA;
import static io.harness.rule.OwnerRule.IGOR;
import static io.harness.rule.OwnerRule.JELENA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import io.harness.utils.RequestField;

import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.secrets.UsageScopeController;
import software.wings.graphql.schema.mutation.cloudProvider.aws.QLAwsCloudProviderInput;
import software.wings.graphql.schema.mutation.cloudProvider.aws.QLAwsCrossAccountAttributes;
import software.wings.graphql.schema.mutation.cloudProvider.aws.QLAwsManualCredentials;
import software.wings.graphql.schema.mutation.cloudProvider.aws.QLUpdateAwsCloudProviderInput;
import software.wings.graphql.schema.mutation.cloudProvider.aws.QLUpdateAwsCrossAccountAttributes;
import software.wings.graphql.schema.mutation.cloudProvider.aws.QLUpdateAwsManualCredentials;
import software.wings.graphql.schema.mutation.cloudProvider.aws.QLUpdateIrsaCredentials;
import software.wings.graphql.schema.type.cloudProvider.aws.QLAwsCredentialsType;
import software.wings.graphql.schema.type.secrets.QLUsageScope;
import software.wings.security.UsageRestrictions;

import java.sql.SQLException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AwsDataFetcherHelperTest extends WingsBaseTest {
  private static final String NAME = "K8S";
  private static final String ACCOUNT_ID = "777";
  private static final String DELEGATE = "DELEGATE";
  public static final String EXTERN_ID = "EXTERN_ID";
  public static final String ARN = "ARN";
  public static final String SECRET_KEY = "SECRET_KEY";
  public static final String ACCESS_KEY = "ACCESS_KEY";
  public static final String DEFAULT_REGION = "DEFAULT_REGION";

  @Mock private UsageScopeController usageScopeController;

  @InjectMocks private AwsDataFetcherHelper helper = new AwsDataFetcherHelper();

  @Before
  public void setup() throws SQLException {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void toSettingAttributeReturnValue() {
    QLAwsCloudProviderInput input =
        QLAwsCloudProviderInput.builder()
            .name(RequestField.ofNullable(NAME))
            .credentialsType(RequestField.ofNullable(QLAwsCredentialsType.MANUAL))
            .manualCredentials(RequestField.ofNullable(QLAwsManualCredentials.builder()
                                                           .accessKey(RequestField.ofNullable(ACCESS_KEY))
                                                           .accessKeySecretId(RequestField.ofNull())
                                                           .secretKeySecretId(RequestField.ofNullable(SECRET_KEY))
                                                           .build()))
            .crossAccountAttributes(
                RequestField.ofNullable(QLAwsCrossAccountAttributes.builder()
                                            .assumeCrossAccountRole(RequestField.ofNullable(Boolean.TRUE))
                                            .crossAccountRoleArn(RequestField.ofNullable(ARN))
                                            .externalId(RequestField.ofNullable(EXTERN_ID))
                                            .build()))
            .defaultRegion(RequestField.ofNullable(DEFAULT_REGION))
            .build();

    SettingAttribute setting = helper.toSettingAttribute(input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
    assertThat(setting.getName()).isEqualTo(NAME);
    assertThat(setting.getValue()).isInstanceOf(AwsConfig.class);
    AwsConfig config = (AwsConfig) setting.getValue();
    assertThat(config.getAccessKey()).isEqualTo(ACCESS_KEY.toCharArray());
    assertThat(config.getEncryptedSecretKey()).isEqualTo(SECRET_KEY);
    assertThat(config.getCrossAccountAttributes()).isNotNull();
    assertThat(config.getCrossAccountAttributes().getCrossAccountRoleArn()).isEqualTo(ARN);
    assertThat(config.getCrossAccountAttributes().getExternalId()).isEqualTo(EXTERN_ID);
    assertThat(config.getDefaultRegion()).isEqualTo(DEFAULT_REGION);
    assertThat(config.isUseEc2IamCredentials()).isFalse();
    assertThat(config.isUseIRSA()).isFalse();
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void toSettingAttributeWithEmptyEc2Input() {
    QLAwsCloudProviderInput input = QLAwsCloudProviderInput.builder()
                                        .name(RequestField.absent())
                                        .credentialsType(RequestField.ofNullable(QLAwsCredentialsType.EC2_IAM))
                                        .ec2IamCredentials(RequestField.ofNull())
                                        .crossAccountAttributes(RequestField.absent())
                                        .build();

    SettingAttribute setting = helper.toSettingAttribute(input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void toSettingAttributeWithEmptyManualInput() {
    QLAwsCloudProviderInput input = QLAwsCloudProviderInput.builder()
                                        .name(RequestField.absent())
                                        .credentialsType(RequestField.ofNullable(QLAwsCredentialsType.MANUAL))
                                        .manualCredentials(RequestField.ofNull())
                                        .crossAccountAttributes(RequestField.absent())
                                        .build();

    SettingAttribute setting = helper.toSettingAttribute(input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = JELENA)
  @Category(UnitTests.class)
  public void toSettingAttributeWithEmptyIrsaInput() {
    QLAwsCloudProviderInput input = QLAwsCloudProviderInput.builder()
                                        .name(RequestField.absent())
                                        .credentialsType(RequestField.ofNullable(QLAwsCredentialsType.IRSA))
                                        .irsaCredentials(RequestField.ofNull())
                                        .crossAccountAttributes(RequestField.absent())
                                        .build();

    SettingAttribute setting = helper.toSettingAttribute(input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void toSettingAttributeWithEmptyXAccountRoleInput() {
    QLAwsCloudProviderInput input = QLAwsCloudProviderInput.builder()
                                        .name(RequestField.absent())
                                        .credentialsType(RequestField.absent())
                                        .crossAccountAttributes(RequestField.ofNull())
                                        .build();

    SettingAttribute setting = helper.toSettingAttribute(input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void updateSettingAttributePerformance() {
    QLUpdateAwsCloudProviderInput input =
        QLUpdateAwsCloudProviderInput.builder()
            .name(RequestField.ofNullable(NAME))
            .credentialsType(RequestField.ofNullable(QLAwsCredentialsType.MANUAL))
            .manualCredentials(RequestField.ofNullable(QLUpdateAwsManualCredentials.builder()
                                                           .accessKey(RequestField.ofNullable(ACCESS_KEY))
                                                           .accessKeySecretId(RequestField.ofNull())
                                                           .secretKeySecretId(RequestField.ofNullable(SECRET_KEY))
                                                           .build()))
            .crossAccountAttributes(
                RequestField.ofNullable(QLUpdateAwsCrossAccountAttributes.builder()
                                            .assumeCrossAccountRole(RequestField.ofNullable(Boolean.TRUE))
                                            .crossAccountRoleArn(RequestField.ofNullable(ARN))
                                            .externalId(RequestField.ofNullable(EXTERN_ID))
                                            .build()))
            .defaultRegion(RequestField.ofNullable(DEFAULT_REGION))
            .build();

    SettingAttribute setting = SettingAttribute.Builder.aSettingAttribute()
                                   .withValue(AwsConfig.builder().useEncryptedAccessKey(true).build())
                                   .build();
    helper.updateSettingAttribute(setting, input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
    assertThat(setting.getName()).isEqualTo(NAME);
    assertThat(setting.getValue()).isInstanceOf(AwsConfig.class);
    AwsConfig config = (AwsConfig) setting.getValue();
    assertThat(config.getAccessKey()).isEqualTo(ACCESS_KEY.toCharArray());
    assertThat(config.getEncryptedSecretKey()).isEqualTo(SECRET_KEY);
    assertThat(config.getCrossAccountAttributes()).isNotNull();
    assertThat(config.getCrossAccountAttributes().getCrossAccountRoleArn()).isEqualTo(ARN);
    assertThat(config.getCrossAccountAttributes().getExternalId()).isEqualTo(EXTERN_ID);
    assertThat(config.getDefaultRegion()).isEqualTo(DEFAULT_REGION);
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void updateSettingAttributeWithEmptyInput() {
    QLUpdateAwsCloudProviderInput input = QLUpdateAwsCloudProviderInput.builder()
                                              .name(RequestField.absent())
                                              .crossAccountAttributes(RequestField.absent())
                                              .ec2IamCredentials(RequestField.absent())
                                              .manualCredentials(RequestField.absent())
                                              .credentialsType(RequestField.absent())
                                              .build();

    SettingAttribute setting =
        SettingAttribute.Builder.aSettingAttribute().withValue(AwsConfig.builder().build()).build();
    helper.updateSettingAttribute(setting, input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void toSettingAttributeAccessKeyAndAccessKeySecretId() {
    assertThatThrownBy(() -> toSettingAttributeAccessKeyAndAccessKeySecretId(ACCESS_KEY, ACCESS_KEY))
        .hasMessageContaining("Cannot set both value and secret reference for accessKey field");

    assertThatThrownBy(() -> toSettingAttributeAccessKeyAndAccessKeySecretId(null, null))
        .hasMessageContaining("One of fields 'accessKey' or 'accessKeySecretId' is required");

    assertSettingAttributeAccessKey(
        toSettingAttributeAccessKeyAndAccessKeySecretId(ACCESS_KEY, null), ACCESS_KEY.toCharArray(), null);
    assertSettingAttributeAccessKey(
        toSettingAttributeAccessKeyAndAccessKeySecretId(null, ACCESS_KEY), null, ACCESS_KEY);
  }

  private SettingAttribute toSettingAttributeAccessKeyAndAccessKeySecretId(String accessKey, String accessKeySecretId) {
    final QLAwsCloudProviderInput input =
        QLAwsCloudProviderInput.builder()
            .name(RequestField.ofNullable(NAME))
            .credentialsType(RequestField.ofNullable(QLAwsCredentialsType.MANUAL))
            .manualCredentials(
                RequestField.ofNullable(QLAwsManualCredentials.builder()
                                            .accessKey(RequestField.ofNullable(accessKey))
                                            .accessKeySecretId(RequestField.ofNullable(accessKeySecretId))
                                            .secretKeySecretId(RequestField.ofNullable(SECRET_KEY))
                                            .build()))
            .crossAccountAttributes(
                RequestField.ofNullable(QLAwsCrossAccountAttributes.builder()
                                            .assumeCrossAccountRole(RequestField.ofNullable(Boolean.TRUE))
                                            .crossAccountRoleArn(RequestField.ofNullable(ARN))
                                            .externalId(RequestField.ofNullable(EXTERN_ID))
                                            .build()))
            .build();

    return helper.toSettingAttribute(input, ACCOUNT_ID);
  }

  private void assertSettingAttributeAccessKey(
      SettingAttribute attribute, char[] accessKey, String encryptedAccessKey) {
    AwsConfig attributeValue = (AwsConfig) attribute.getValue();
    assertThat(attributeValue.getAccessKey()).isEqualTo(accessKey);
    assertThat(attributeValue.getEncryptedAccessKey()).isEqualTo(encryptedAccessKey);
    assertThat(attributeValue.isUseEncryptedAccessKey()).isEqualTo(encryptedAccessKey != null);
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void updateSettingAttributeAccessKeyAndAccessKeySecretId() {
    assertThatThrownBy(() -> updateSettingAttributeAccessKeyAndAccessKeySecretId(ACCESS_KEY, ACCESS_KEY))
        .hasMessageContaining("Cannot set both value and secret reference for accessKey field");

    assertSettingAttributeAccessKey(
        updateSettingAttributeAccessKeyAndAccessKeySecretId(ACCESS_KEY, null), ACCESS_KEY.toCharArray(), null);
    assertSettingAttributeAccessKey(
        updateSettingAttributeAccessKeyAndAccessKeySecretId(null, ACCESS_KEY), null, ACCESS_KEY);
    // Keep existing values
    assertSettingAttributeAccessKey(
        updateSettingAttributeAccessKeyAndAccessKeySecretId(null, null), ACCESS_KEY.toCharArray(), ACCESS_KEY);
  }

  private SettingAttribute updateSettingAttributeAccessKeyAndAccessKeySecretId(
      String accessKey, String accessKeySecretId) {
    final QLUpdateAwsCloudProviderInput input =
        QLUpdateAwsCloudProviderInput.builder()
            .name(RequestField.ofNullable(NAME))
            .credentialsType(RequestField.ofNullable(QLAwsCredentialsType.MANUAL))
            .manualCredentials(
                RequestField.ofNullable(QLUpdateAwsManualCredentials.builder()
                                            .accessKey(RequestField.ofNullable(accessKey))
                                            .accessKeySecretId(RequestField.ofNullable(accessKeySecretId))
                                            .secretKeySecretId(RequestField.ofNullable(SECRET_KEY))
                                            .build()))
            .crossAccountAttributes(
                RequestField.ofNullable(QLUpdateAwsCrossAccountAttributes.builder()
                                            .assumeCrossAccountRole(RequestField.ofNullable(Boolean.TRUE))
                                            .crossAccountRoleArn(RequestField.ofNullable(ARN))
                                            .externalId(RequestField.ofNullable(EXTERN_ID))
                                            .build()))
            .build();

    SettingAttribute setting = SettingAttribute.Builder.aSettingAttribute()
                                   .withValue(AwsConfig.builder()
                                                  .accessKey(ACCESS_KEY.toCharArray())
                                                  .encryptedAccessKey(ACCESS_KEY)
                                                  .useEncryptedAccessKey(true)
                                                  .build())
                                   .build();
    helper.updateSettingAttribute(setting, input, ACCOUNT_ID);
    return setting;
  }

  @Test
  @Owner(developers = JELENA)
  @Category(UnitTests.class)
  public void updateSettingAttributeIrsaUsageScope() {
    final QLUpdateAwsCloudProviderInput input =
        QLUpdateAwsCloudProviderInput.builder()
            .name(RequestField.ofNullable(NAME))
            .credentialsType(RequestField.ofNullable(QLAwsCredentialsType.IRSA))
            .irsaCredentials(RequestField.ofNullable(
                QLUpdateIrsaCredentials.builder().delegateSelector(RequestField.ofNullable("DELEGATE")).build()))
            .crossAccountAttributes(
                RequestField.ofNullable(QLUpdateAwsCrossAccountAttributes.builder()
                                            .assumeCrossAccountRole(RequestField.ofNullable(Boolean.TRUE))
                                            .crossAccountRoleArn(RequestField.ofNullable(ARN))
                                            .externalId(RequestField.ofNullable(EXTERN_ID))
                                            .build()))
            .build();

    UsageRestrictions usageRestrictions = new UsageRestrictions();
    QLUsageScope qlUsageScope = QLUsageScope.builder().build();
    doReturn(qlUsageScope).when(usageScopeController).populateUsageScope(usageRestrictions);
    SettingAttribute setting = SettingAttribute.Builder.aSettingAttribute()
                                   .withValue(AwsConfig.builder().useIRSA(true).build())
                                   .withUsageRestrictions(usageRestrictions)
                                   .build();

    helper.updateSettingAttribute(setting, input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
    assertThat(setting.getName()).isEqualTo(NAME);
    assertThat(setting.getUsageRestrictions()).isEqualTo(usageRestrictions);
    assertThat(setting.getValue()).isInstanceOf(AwsConfig.class);
    AwsConfig config = (AwsConfig) setting.getValue();
    assertThat(config.isUseIRSA()).isTrue();
    assertThat(config.getCrossAccountAttributes()).isNotNull();
    assertThat(config.getCrossAccountAttributes().getCrossAccountRoleArn()).isEqualTo(ARN);
    assertThat(config.getCrossAccountAttributes().getExternalId()).isEqualTo(EXTERN_ID);
  }
}
