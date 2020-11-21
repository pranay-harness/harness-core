package software.wings.service.impl.security.customsecretsmanager;

import static io.harness.rule.OwnerRule.UTKARSH;

import static software.wings.service.impl.security.customsecretsmanager.CustomSecretsManagerUtils.obtainConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.beans.delegation.ShellScriptParameters;
import software.wings.delegatetasks.validation.ShellScriptValidationHandler;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerConfig;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class CustomSecretsManagerValidationImplTest extends CategoryTest {
  @Mock private ShellScriptValidationHandler shellScriptValidationHandler;
  @Inject @InjectMocks private CustomSecretsManagerValidationImpl customSecretsManagerDelegateService;

  @Before
  public void setup() {
    initMocks(this);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void test_isExecutableOnDelegate_shouldReturnTrue() {
    CustomSecretsManagerConfig config = obtainConfig(null);
    when(shellScriptValidationHandler.handle(any(ShellScriptParameters.class))).thenReturn(true);
    boolean isValidated = customSecretsManagerDelegateService.isExecutableOnDelegate(config);
    assertThat(isValidated).isTrue();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void test_isExecutableOnDelegate_shouldReturnFalse() {
    CustomSecretsManagerConfig config = obtainConfig(null);
    when(shellScriptValidationHandler.handle(any(ShellScriptParameters.class))).thenReturn(false);
    boolean isValidated = customSecretsManagerDelegateService.isExecutableOnDelegate(config);
    assertThat(isValidated).isFalse();
  }
}
