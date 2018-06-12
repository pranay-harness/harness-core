package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;
import static software.wings.service.impl.security.SecretManagementDelegateServiceImpl.getVaultRestClient;
import static software.wings.service.impl.security.VaultServiceImpl.VAULT_VAILDATION_URL;

import retrofit2.Call;
import retrofit2.Response;
import software.wings.beans.DelegateTask;
import software.wings.beans.VaultConfig;
import software.wings.service.impl.security.VaultReadResponse;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.io.IOException;
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
                               return SettingVariableTypes.VAULT + ":" + vaultConfig.getVaultUrl();
                             })
                             .findFirst()
                             .orElse(null));
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    for (Object parmeter : getParameters()) {
      if (parmeter instanceof VaultConfig) {
        VaultConfig vaultConfig = (VaultConfig) parmeter;
        Call<VaultReadResponse> request = getVaultRestClient(vaultConfig)
                                              .readSecret(String.valueOf(vaultConfig.getAuthToken()),
                                                  SettingVariableTypes.VAULT + "/" + VAULT_VAILDATION_URL);

        try {
          Response<VaultReadResponse> response = request.execute();
          return singletonList(DelegateConnectionResult.builder()
                                   .criteria(getCriteria().get(0))
                                   .validated(response.isSuccessful())
                                   .build());
        } catch (IOException e) {
          return singletonList(
              DelegateConnectionResult.builder().criteria(getCriteria().get(0)).validated(false).build());
        }
      }
    }
    return super.validate();
  }
}
