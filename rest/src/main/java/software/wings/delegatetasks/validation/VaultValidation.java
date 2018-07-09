package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import software.wings.beans.DelegateTask;
import software.wings.beans.VaultConfig;
import software.wings.service.intfc.security.EncryptionConfig;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by brett on 11/2/17
 */
public class VaultValidation extends AbstractSecretManagerValidation {
  public VaultValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(Arrays.stream(getParameters())
                             .filter(o -> o instanceof VaultConfig)
                             .map(obj -> {
                               VaultConfig vaultConfig = (VaultConfig) obj;
                               return SettingVariableTypes.VAULT + ":" + vaultConfig.getVaultUrl();
                             })
                             .findFirst()
                             .orElse(null));
  }

  @Override
  protected EncryptionConfig getEncryptionConfig() {
    for (Object parmeter : getParameters()) {
      if (parmeter instanceof VaultConfig) {
        return (VaultConfig) parmeter;
      }
    }
    return super.getEncryptionConfig();
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    return singletonList(super.validateSecretManager());
  }
}
