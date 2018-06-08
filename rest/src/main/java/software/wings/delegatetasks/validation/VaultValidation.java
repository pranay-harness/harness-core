package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import software.wings.beans.DelegateTask;
import software.wings.beans.VaultConfig;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by brett on 11/2/17
 */
public class VaultValidation extends AbstractDelegateValidateTask {
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
                               return vaultConfig.getVaultUrl();
                             })
                             .findFirst()
                             .orElse(null));
  }
}
