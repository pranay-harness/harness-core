package software.wings.service.impl.security.customsecretsmanager;

import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static software.wings.service.impl.security.customsecretsmanager.CustomSecretsManagerUtils.obtainConfig;
import static software.wings.settings.SettingValue.SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES;

import com.google.inject.Inject;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.expression.ExpressionEvaluator;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.annotation.EncryptableSetting;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerConfig;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManager;

public class CustomSecretsManagerEncryptionServiceImplTest extends CategoryTest {
  @Mock private CustomSecretsManagerConnectorHelper customSecretsManagerConnectorHelper;
  @Mock private SecretManager secretManager;
  @Mock private ManagerDecryptionService managerDecryptionService;
  @Mock private ExpressionEvaluator expressionEvaluator;
  @Inject @InjectMocks private CustomSecretsManagerEncryptionServiceImpl customSecretsManagerEncryptionService;

  @Before
  public void setup() {
    initMocks(this);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void test_buildEncryptedDataDetail() {
    CustomSecretsManagerConfig config = obtainConfig(HOST_CONNECTION_ATTRIBUTES);
    config.setConnectorTemplatized(true);
    EncryptedData encryptedData = mock(EncryptedData.class);
    String shellScript = config.getCustomSecretsManagerShellScript().getScriptString();
    when(expressionEvaluator.substitute(eq(shellScript), any())).thenReturn(shellScript);
    EncryptedDataDetail encryptedDataDetail =
        customSecretsManagerEncryptionService.buildEncryptedDataDetail(encryptedData, config);
    assertThat(encryptedDataDetail).isNotNull();
    assertThat(encryptedDataDetail.getEncryptionConfig()).isEqualTo(config);
    assertThat(encryptedDataDetail.getEncryptedData()).isNotNull();

    verify(expressionEvaluator, times(1)).substitute(eq(shellScript), any());
    verify(customSecretsManagerConnectorHelper, times(1)).setConnectorInConfig(eq(config), any());
    verify(managerDecryptionService, times(1)).decrypt(any(EncryptableSetting.class), any());
    verify(secretManager, times(1)).getEncryptionDetails(any(EncryptableSetting.class));
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void test_validateSecret() {
    CustomSecretsManagerConfig config = obtainConfig(HOST_CONNECTION_ATTRIBUTES);
    config.setConnectorTemplatized(true);
    EncryptedData encryptedData = mock(EncryptedData.class);
    String shellScript = config.getCustomSecretsManagerShellScript().getScriptString();
    when(expressionEvaluator.substitute(eq(shellScript), any())).thenReturn(shellScript);
    customSecretsManagerEncryptionService.validateSecret(encryptedData, config);

    verify(expressionEvaluator, times(1)).substitute(eq(shellScript), any());
    verify(customSecretsManagerConnectorHelper, times(1)).setConnectorInConfig(any(), any());
    verify(managerDecryptionService, times(1)).decrypt(any(EncryptableSetting.class), any());
    verify(secretManager, times(1)).getEncryptionDetails(any(EncryptableSetting.class));
  }
}
