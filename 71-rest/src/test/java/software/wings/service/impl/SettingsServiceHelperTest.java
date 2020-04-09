package software.wings.service.impl;

import static io.harness.rule.OwnerRule.GARVIT;
import static io.harness.rule.OwnerRule.RAGHU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.SettingAttribute.SettingCategory.CLOUD_PROVIDER;
import static software.wings.beans.SettingAttribute.SettingCategory.CONNECTOR;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.APMVerificationConfig;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.BugsnagConfig;
import software.wings.beans.DatadogConfig;
import software.wings.beans.DynaTraceConfig;
import software.wings.beans.ElkConfig;
import software.wings.beans.FeatureName;
import software.wings.beans.GitConfig;
import software.wings.beans.InstanaConfig;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.PrometheusConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SplunkConfig;
import software.wings.beans.SumoConfig;
import software.wings.beans.config.LogzConfig;
import software.wings.beans.settings.azureartifacts.AzureArtifactsPATConfig;
import software.wings.beans.settings.helm.HttpHelmRepoConfig;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManager;

public class SettingsServiceHelperTest extends WingsBaseTest {
  private static final String ACCOUNT_ID = "ACCOUNT_ID";
  private static final String PAT = "PAT";
  private static final String RANDOM = "RANDOM";

  @Mock private SecretManager secretManager;
  @Mock private ManagerDecryptionService managerDecryptionService;
  @Mock private FeatureFlagService featureFlagService;

  @InjectMocks @Inject private SettingServiceHelper settingServiceHelper;

  @Before
  public void setup() {
    when(featureFlagService.isEnabled(FeatureName.CONNECTORS_REF_SECRETS, ACCOUNT_ID)).thenReturn(true);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testHasReferencedSecrets() {
    assertThat(settingServiceHelper.hasReferencedSecrets(null)).isFalse();
    SettingAttribute settingAttribute = aSettingAttribute().withCategory(CONNECTOR).build();
    assertThat(settingServiceHelper.hasReferencedSecrets(settingAttribute)).isFalse();
    settingAttribute.setValue(AzureArtifactsPATConfig.builder().build());
    assertThat(settingServiceHelper.hasReferencedSecrets(settingAttribute)).isFalse();

    settingAttribute.setAccountId(ACCOUNT_ID);
    when(featureFlagService.isEnabled(FeatureName.CONNECTORS_REF_SECRETS, ACCOUNT_ID)).thenReturn(false);
    assertThat(settingServiceHelper.hasReferencedSecrets(settingAttribute)).isFalse();
    when(featureFlagService.isEnabled(FeatureName.CONNECTORS_REF_SECRETS, ACCOUNT_ID)).thenReturn(true);
    assertThat(settingServiceHelper.hasReferencedSecrets(settingAttribute)).isTrue();

    settingAttribute.setValue(HttpHelmRepoConfig.builder().build());
    assertThat(settingServiceHelper.hasReferencedSecrets(settingAttribute)).isTrue();

    settingAttribute.setValue(GitConfig.builder().build());
    assertThat(settingServiceHelper.hasReferencedSecrets(settingAttribute)).isTrue();

    settingAttribute.setValue(SmtpConfig.builder().build());
    assertThat(settingServiceHelper.hasReferencedSecrets(settingAttribute)).isTrue();

    settingAttribute.setCategory(CLOUD_PROVIDER);
    assertThat(settingServiceHelper.hasReferencedSecrets(settingAttribute)).isTrue();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testHasReferencedSecretsVerificatonConnectors() {
    SettingAttribute settingAttribute = aSettingAttribute().withCategory(CONNECTOR).withAccountId(ACCOUNT_ID).build();
    settingAttribute.setValue(AppDynamicsConfig.builder().build());
    assertThat(settingServiceHelper.hasReferencedSecrets(settingAttribute)).isTrue();

    settingAttribute.setValue(NewRelicConfig.builder().build());
    assertThat(settingServiceHelper.hasReferencedSecrets(settingAttribute)).isTrue();

    settingAttribute.setValue(InstanaConfig.builder().build());
    assertThat(settingServiceHelper.hasReferencedSecrets(settingAttribute)).isTrue();

    settingAttribute.setValue(DynaTraceConfig.builder().build());
    assertThat(settingServiceHelper.hasReferencedSecrets(settingAttribute)).isTrue();

    settingAttribute.setValue(PrometheusConfig.builder().build());
    assertThat(settingServiceHelper.hasReferencedSecrets(settingAttribute)).isTrue();

    settingAttribute.setValue(DatadogConfig.builder().build());
    assertThat(settingServiceHelper.hasReferencedSecrets(settingAttribute)).isTrue();

    settingAttribute.setValue(BugsnagConfig.builder().build());
    assertThat(settingServiceHelper.hasReferencedSecrets(settingAttribute)).isTrue();

    settingAttribute.setValue(ElkConfig.builder().build());
    assertThat(settingServiceHelper.hasReferencedSecrets(settingAttribute)).isTrue();

    settingAttribute.setValue(SplunkConfig.builder().build());
    assertThat(settingServiceHelper.hasReferencedSecrets(settingAttribute)).isTrue();

    settingAttribute.setValue(SumoConfig.builder().build());
    assertThat(settingServiceHelper.hasReferencedSecrets(settingAttribute)).isTrue();

    settingAttribute.setValue(LogzConfig.builder().build());
    assertThat(settingServiceHelper.hasReferencedSecrets(settingAttribute)).isTrue();

    settingAttribute.setValue(new APMVerificationConfig());
    assertThat(settingServiceHelper.hasReferencedSecrets(settingAttribute)).isFalse();
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testUpdateEncryptedFieldsInResponse() {
    SettingAttribute settingAttribute = aSettingAttribute().withAccountId(ACCOUNT_ID).withCategory(CONNECTOR).build();
    // Should not throw an exception.
    settingServiceHelper.updateEncryptedFieldsInResponse(settingAttribute, true);

    AzureArtifactsPATConfig azureArtifactsPATConfig =
        AzureArtifactsPATConfig.builder().pat(RANDOM.toCharArray()).encryptedPat(PAT).build();
    settingAttribute.setValue(azureArtifactsPATConfig);
    settingServiceHelper.updateEncryptedFieldsInResponse(settingAttribute, true);
    assertThat(azureArtifactsPATConfig.getEncryptedPat()).isNull();
    assertThat(azureArtifactsPATConfig.getPat()).isEqualTo(PAT.toCharArray());

    SplunkConfig splunkConfig = SplunkConfig.builder().password(RANDOM.toCharArray()).encryptedPassword(PAT).build();
    settingAttribute.setValue(splunkConfig);
    settingServiceHelper.updateEncryptedFieldsInResponse(settingAttribute, true);
    assertThat(splunkConfig.getEncryptedPassword()).isNull();
    assertThat(splunkConfig.getPassword()).isEqualTo(PAT.toCharArray());
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testUpdateReferencedSecrets() {
    SettingAttribute settingAttribute = aSettingAttribute().withAccountId(ACCOUNT_ID).withCategory(CONNECTOR).build();
    // Early return without exceptions.
    settingServiceHelper.updateReferencedSecrets(settingAttribute);

    AzureArtifactsPATConfig azureArtifactsPATConfig =
        AzureArtifactsPATConfig.builder().pat(RANDOM.toCharArray()).encryptedPat(PAT).build();
    settingAttribute.setValue(azureArtifactsPATConfig);
    settingServiceHelper.updateReferencedSecrets(settingAttribute);
    verify(managerDecryptionService, times(1)).decrypt(eq(azureArtifactsPATConfig), any());
    assertThat(azureArtifactsPATConfig.isDecrypted()).isFalse();

    azureArtifactsPATConfig.setDecrypted(true);
    settingServiceHelper.updateReferencedSecrets(settingAttribute);
    verify(managerDecryptionService, times(2)).decrypt(eq(azureArtifactsPATConfig), any());
    assertThat(azureArtifactsPATConfig.isDecrypted()).isTrue();
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testResetEncryptedFields() {
    AzureArtifactsPATConfig azureArtifactsPATConfig =
        AzureArtifactsPATConfig.builder().pat(RANDOM.toCharArray()).encryptedPat(PAT).build();
    settingServiceHelper.resetEncryptedFields(azureArtifactsPATConfig);
    assertThat(azureArtifactsPATConfig.getPat()).isNull();
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testCopyToEncryptedRefFields() {
    AzureArtifactsPATConfig azureArtifactsPATConfig =
        AzureArtifactsPATConfig.builder().pat(null).encryptedPat(PAT).build();
    settingServiceHelper.copyToEncryptedRefFields(azureArtifactsPATConfig);
    assertThat(azureArtifactsPATConfig.getPat()).isNull();
    assertThat(azureArtifactsPATConfig.getEncryptedPat()).isEqualTo(PAT);

    azureArtifactsPATConfig = AzureArtifactsPATConfig.builder().pat(RANDOM.toCharArray()).encryptedPat(PAT).build();
    settingServiceHelper.copyToEncryptedRefFields(azureArtifactsPATConfig);
    assertThat(azureArtifactsPATConfig.getPat()).isNull();
    assertThat(azureArtifactsPATConfig.getEncryptedPat()).isEqualTo(RANDOM);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testCopyFromEncryptedRefFields() {
    AzureArtifactsPATConfig azureArtifactsPATConfig =
        AzureArtifactsPATConfig.builder().pat(RANDOM.toCharArray()).encryptedPat(PAT).build();
    settingServiceHelper.copyFromEncryptedRefFields(azureArtifactsPATConfig);
    assertThat(azureArtifactsPATConfig.getPat()).isEqualTo(PAT.toCharArray());
    assertThat(azureArtifactsPATConfig.getEncryptedPat()).isNull();
  }
}
