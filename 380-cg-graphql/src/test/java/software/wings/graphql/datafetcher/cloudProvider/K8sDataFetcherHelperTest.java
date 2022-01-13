/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.cloudProvider;

import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.IGOR;
import static io.harness.rule.OwnerRule.TATHAGAT;

import static software.wings.utils.WingsTestConstants.USER_NAME;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import io.harness.utils.RequestField;

import software.wings.WingsBaseTest;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.secrets.UsageScopeController;
import software.wings.graphql.schema.mutation.cloudProvider.k8s.QLInheritClusterDetails;
import software.wings.graphql.schema.mutation.cloudProvider.k8s.QLK8sCloudProviderInput;
import software.wings.graphql.schema.mutation.cloudProvider.k8s.QLManualClusterDetails;
import software.wings.graphql.schema.mutation.cloudProvider.k8s.QLUpdateInheritClusterDetails;
import software.wings.graphql.schema.mutation.cloudProvider.k8s.QLUpdateK8sCloudProviderInput;
import software.wings.graphql.schema.mutation.cloudProvider.k8s.QLUpdateManualClusterDetails;
import software.wings.graphql.schema.mutation.cloudProvider.k8s.QLUpdateUsernameAndPasswordAuthentication;
import software.wings.graphql.schema.mutation.cloudProvider.k8s.QLUsernameAndPasswordAuthentication;
import software.wings.graphql.schema.type.cloudProvider.k8s.QLClusterDetailsType;
import software.wings.graphql.schema.type.cloudProvider.k8s.QLManualClusterDetailsAuthenticationType;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class K8sDataFetcherHelperTest extends WingsBaseTest {
  private static final String NAME = "K8S";
  private static final String DELEGATE = "DELEGATE";
  private static final String ACCOUNT_ID = "777";
  private static final String MASTER_URL = "MASTER_URL";
  private static final String PASSWORD_SECRET_ID = "PASSWORD";

  @Mock private UsageScopeController usageScopeController;

  @InjectMocks private K8sDataFetcherHelper helper = new K8sDataFetcherHelper();

  @Before
  public void setup() throws SQLException {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void toSettingAttributeReturnValue() {
    final QLK8sCloudProviderInput input =
        QLK8sCloudProviderInput.builder()
            .name(RequestField.ofNullable(NAME))
            .skipValidation(RequestField.ofNullable(Boolean.TRUE))
            .clusterDetailsType(RequestField.ofNullable(QLClusterDetailsType.INHERIT_CLUSTER_DETAILS))
            .inheritClusterDetails(RequestField.ofNullable(
                QLInheritClusterDetails.builder()
                    .delegateSelectors(RequestField.ofNullable(new HashSet<>(Collections.singletonList(DELEGATE))))
                    .delegateName(RequestField.ofNull())
                    .build()))
            .build();

    SettingAttribute setting = helper.toSettingAttribute(input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
    assertThat(setting.getName()).isEqualTo(NAME);
    assertThat(setting.getValue()).isInstanceOf(KubernetesClusterConfig.class);
    KubernetesClusterConfig config = (KubernetesClusterConfig) setting.getValue();
    assertThat(config.isSkipValidation()).isEqualTo(Boolean.TRUE);
    assertThat(config.isUseKubernetesDelegate()).isTrue();
    assertThat(config.getDelegateSelectors().iterator().next()).isEqualTo(DELEGATE);
  }
  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void toSettingAttributeDelegateNamePresentSelectorsNull() {
    final QLK8sCloudProviderInput input =
        QLK8sCloudProviderInput.builder()
            .name(RequestField.ofNullable(NAME))
            .skipValidation(RequestField.ofNullable(Boolean.TRUE))
            .clusterDetailsType(RequestField.ofNullable(QLClusterDetailsType.INHERIT_CLUSTER_DETAILS))
            .inheritClusterDetails(RequestField.ofNullable(QLInheritClusterDetails.builder()
                                                               .delegateSelectors(RequestField.ofNull())
                                                               .delegateName(RequestField.ofNullable(DELEGATE))
                                                               .build()))
            .build();

    SettingAttribute setting = helper.toSettingAttribute(input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
    KubernetesClusterConfig config = (KubernetesClusterConfig) setting.getValue();
    assertThat(config.getDelegateSelectors().iterator().next()).isEqualTo(DELEGATE);
    assertThat(config.getDelegateName()).isEqualTo(DELEGATE);
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void toSettingAttributeWithEmptyInput() {
    final QLK8sCloudProviderInput input = QLK8sCloudProviderInput.builder()
                                              .name(RequestField.ofNull())
                                              .skipValidation(RequestField.ofNull())
                                              .clusterDetailsType(RequestField.absent())
                                              .inheritClusterDetails(RequestField.ofNull())
                                              .manualClusterDetails(RequestField.ofNull())
                                              .build();

    SettingAttribute setting = helper.toSettingAttribute(input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void toSettingAttributeWithNoClusterDatailsType() {
    final QLK8sCloudProviderInput input = QLK8sCloudProviderInput.builder()
                                              .name(RequestField.ofNull())
                                              .skipValidation(RequestField.ofNull())
                                              .clusterDetailsType(RequestField.ofNull())
                                              .inheritClusterDetails(RequestField.ofNull())
                                              .manualClusterDetails(RequestField.ofNull())
                                              .build();

    SettingAttribute setting = helper.toSettingAttribute(input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void updateSettingAttributePerformance() {
    final QLUpdateK8sCloudProviderInput input =
        QLUpdateK8sCloudProviderInput.builder()
            .name(RequestField.ofNullable(NAME))
            .skipValidation(RequestField.ofNullable(Boolean.TRUE))
            .clusterDetailsType(RequestField.ofNullable(QLClusterDetailsType.INHERIT_CLUSTER_DETAILS))
            .inheritClusterDetails(RequestField.ofNullable(
                QLUpdateInheritClusterDetails.builder()
                    .delegateSelectors(RequestField.ofNullable(new HashSet<>(Collections.singletonList(DELEGATE))))
                    .delegateName(RequestField.ofNull())
                    .build()))
            .build();

    SettingAttribute setting =
        SettingAttribute.Builder.aSettingAttribute().withValue(KubernetesClusterConfig.builder().build()).build();
    helper.updateSettingAttribute(setting, input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
    assertThat(setting.getName()).isEqualTo(NAME);
    assertThat(setting.getValue()).isInstanceOf(KubernetesClusterConfig.class);
    KubernetesClusterConfig config = (KubernetesClusterConfig) setting.getValue();
    assertThat(config.isSkipValidation()).isTrue();
    assertThat(config.isUseKubernetesDelegate()).isTrue();
    assertThat(config.getDelegateSelectors().iterator().next()).isEqualTo(DELEGATE);
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void updateSettingAttributeDelegateNamePresentSelectorsNull() {
    final QLUpdateK8sCloudProviderInput input =
        QLUpdateK8sCloudProviderInput.builder()
            .name(RequestField.ofNullable(NAME))
            .skipValidation(RequestField.ofNullable(Boolean.TRUE))
            .clusterDetailsType(RequestField.ofNullable(QLClusterDetailsType.INHERIT_CLUSTER_DETAILS))
            .inheritClusterDetails(RequestField.ofNullable(QLUpdateInheritClusterDetails.builder()
                                                               .delegateSelectors(RequestField.ofNull())
                                                               .delegateName(RequestField.ofNullable(DELEGATE))
                                                               .build()))
            .build();

    SettingAttribute setting =
        SettingAttribute.Builder.aSettingAttribute().withValue(KubernetesClusterConfig.builder().build()).build();
    helper.updateSettingAttribute(setting, input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
    KubernetesClusterConfig config = (KubernetesClusterConfig) setting.getValue();
    assertThat(config.getDelegateSelectors().iterator().next()).isEqualTo(DELEGATE);
    assertThat(config.getDelegateName()).isEqualTo(DELEGATE);
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void updateSettingAttributeWithEmptyInput() {
    final QLUpdateK8sCloudProviderInput input = QLUpdateK8sCloudProviderInput.builder()
                                                    .name(RequestField.ofNull())
                                                    .skipValidation(RequestField.ofNull())
                                                    .clusterDetailsType(RequestField.absent())
                                                    .inheritClusterDetails(RequestField.ofNull())
                                                    .manualClusterDetails(RequestField.ofNull())
                                                    .build();

    SettingAttribute setting =
        SettingAttribute.Builder.aSettingAttribute().withValue(KubernetesClusterConfig.builder().build()).build();
    helper.updateSettingAttribute(setting, input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void updateSettingAttributeWithClusterDetailsType() {
    final QLUpdateK8sCloudProviderInput input = QLUpdateK8sCloudProviderInput.builder()
                                                    .name(RequestField.ofNull())
                                                    .skipValidation(RequestField.ofNull())
                                                    .clusterDetailsType(RequestField.ofNull())
                                                    .inheritClusterDetails(RequestField.ofNull())
                                                    .manualClusterDetails(RequestField.ofNull())
                                                    .build();

    SettingAttribute setting =
        SettingAttribute.Builder.aSettingAttribute().withValue(KubernetesClusterConfig.builder().build()).build();
    helper.updateSettingAttribute(setting, input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void toSettingAttributeUsernameAndUsernameSecretId() {
    assertThatThrownBy(() -> toSettingAttributeUsernameAndUsernameSecretId(USER_NAME, USER_NAME))
        .hasMessageContaining("Cannot set both value and secret reference for username field");

    assertThatThrownBy(() -> toSettingAttributeUsernameAndUsernameSecretId(null, null))
        .hasMessageContaining("One of fields 'userName' or 'userNameSecretId' is required");

    assertSettingAttributeUsername(
        toSettingAttributeUsernameAndUsernameSecretId(USER_NAME, null), USER_NAME.toCharArray(), null);
    assertSettingAttributeUsername(toSettingAttributeUsernameAndUsernameSecretId(null, USER_NAME), null, USER_NAME);
  }

  private SettingAttribute toSettingAttributeUsernameAndUsernameSecretId(String userName, String userNameSecretId) {
    final QLManualClusterDetails clusterDetails =
        QLManualClusterDetails.builder()
            .type(RequestField.ofNullable(QLManualClusterDetailsAuthenticationType.USERNAME_AND_PASSWORD))
            .usernameAndPassword(
                RequestField.ofNullable(QLUsernameAndPasswordAuthentication.builder()
                                            .userName(RequestField.ofNullable(userName))
                                            .userNameSecretId(RequestField.ofNullable(userNameSecretId))
                                            .passwordSecretId(RequestField.ofNullable(PASSWORD_SECRET_ID))
                                            .build()))
            .masterUrl(RequestField.ofNullable(MASTER_URL))
            .build();

    final QLK8sCloudProviderInput input =
        QLK8sCloudProviderInput.builder()
            .name(RequestField.ofNullable(NAME))
            .skipValidation(RequestField.ofNullable(Boolean.TRUE))
            .clusterDetailsType(RequestField.ofNullable(QLClusterDetailsType.MANUAL_CLUSTER_DETAILS))
            .manualClusterDetails(RequestField.ofNullable(clusterDetails))
            .build();

    return helper.toSettingAttribute(input, ACCOUNT_ID);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void updateSettingAttributeUsernameAndUsernameSecretId() {
    assertThatThrownBy(() -> updateSettingAttributeUsernameAndUsernameSecretId(USER_NAME, USER_NAME))
        .hasMessageContaining("Cannot set both value and secret reference for username field");

    assertSettingAttributeUsername(
        updateSettingAttributeUsernameAndUsernameSecretId(USER_NAME, null), USER_NAME.toCharArray(), null);
    assertSettingAttributeUsername(updateSettingAttributeUsernameAndUsernameSecretId(null, USER_NAME), null, USER_NAME);
    // Keep existing values
    assertSettingAttributeUsername(
        updateSettingAttributeUsernameAndUsernameSecretId(null, null), USER_NAME.toCharArray(), USER_NAME);
  }

  private SettingAttribute updateSettingAttributeUsernameAndUsernameSecretId(String userName, String userNameSecretId) {
    final QLUpdateManualClusterDetails clusterDetails =
        QLUpdateManualClusterDetails.builder()
            .type(RequestField.ofNullable(QLManualClusterDetailsAuthenticationType.USERNAME_AND_PASSWORD))
            .usernameAndPassword(
                RequestField.ofNullable(QLUpdateUsernameAndPasswordAuthentication.builder()
                                            .userName(RequestField.ofNullable(userName))
                                            .userNameSecretId(RequestField.ofNullable(userNameSecretId))
                                            .passwordSecretId(RequestField.ofNullable(PASSWORD_SECRET_ID))
                                            .build()))
            .masterUrl(RequestField.ofNullable(MASTER_URL))
            .build();

    final QLUpdateK8sCloudProviderInput input =
        QLUpdateK8sCloudProviderInput.builder()
            .name(RequestField.ofNull())
            .skipValidation(RequestField.ofNull())
            .clusterDetailsType(RequestField.ofNullable(QLClusterDetailsType.MANUAL_CLUSTER_DETAILS))
            .manualClusterDetails(RequestField.ofNullable(clusterDetails))
            .build();

    SettingAttribute setting = SettingAttribute.Builder.aSettingAttribute()
                                   .withValue(KubernetesClusterConfig.builder()
                                                  .username(USER_NAME.toCharArray())
                                                  .encryptedUsername(USER_NAME)
                                                  .useEncryptedUsername(true)
                                                  .build())
                                   .build();
    helper.updateSettingAttribute(setting, input, ACCOUNT_ID);

    return setting;
  }

  private void assertSettingAttributeUsername(SettingAttribute attribute, char[] username, String encryptedUsername) {
    KubernetesClusterConfig attributeValue = (KubernetesClusterConfig) attribute.getValue();
    assertThat(attributeValue.getUsername()).isEqualTo(username);
    assertThat(attributeValue.getEncryptedUsername()).isEqualTo(encryptedUsername);
    assertThat(attributeValue.isUseEncryptedUsername()).isEqualTo(encryptedUsername != null);
  }
}
