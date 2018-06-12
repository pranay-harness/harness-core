package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;
import static software.wings.service.impl.security.SecretManagementDelegateServiceImpl.getVaultRestClient;
import static software.wings.service.impl.security.VaultServiceImpl.VAULT_VAILDATION_URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Response;
import software.wings.beans.DelegateTask;
import software.wings.beans.VaultConfig;
import software.wings.security.EncryptionType;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.security.VaultReadResponse;
import software.wings.service.intfc.security.EncryptionConfig;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by brett on 11/2/17
 */
public class SecretManagerValidation extends AbstractDelegateValidateTask {
  private static final Logger logger = LoggerFactory.getLogger(SecretManagerValidation.class);
  public SecretManagerValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(Arrays.stream(getParameters())
                             .filter(o -> o instanceof List)
                             .map(obj -> {
                               List<EncryptedDataDetail> encryptedDataDetails = (List) obj;
                               String secretManagerUrl = "https://aws.amazon.com/";
                               for (EncryptedDataDetail encryptedDataDetail : encryptedDataDetails) {
                                 EncryptionConfig encryptionConfig = encryptedDataDetail.getEncryptionConfig();
                                 if (encryptedDataDetail.getEncryptionType().equals(EncryptionType.VAULT)) {
                                   secretManagerUrl = SettingVariableTypes.VAULT + ":"
                                       + ((VaultConfig) encryptionConfig).getVaultUrl();
                                   break;
                                 }
                               }
                               return secretManagerUrl;
                             })
                             .findFirst()
                             .orElse(null));
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    List<EncryptedDataDetail> encryptionDetails = (List<EncryptedDataDetail>) getParameters()[3];
    logger.info("Running validation for task {} for encryptionDetails {}", delegateTaskId, encryptionDetails);
    for (EncryptedDataDetail encryptedDataDetail : encryptionDetails) {
      switch (encryptedDataDetail.getEncryptionType()) {
        case KMS:
          return super.validate();

        case VAULT:
          VaultConfig vaultConfig = (VaultConfig) encryptedDataDetail.getEncryptionConfig();
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

        default:
          return super.validate();
      }
    }

    return super.validate();
  }
}
