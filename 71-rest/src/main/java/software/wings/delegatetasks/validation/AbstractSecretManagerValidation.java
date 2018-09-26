package software.wings.delegatetasks.validation;

import static io.harness.network.Http.connectableHttpUrl;
import static java.util.Collections.singletonList;
import static software.wings.service.impl.security.SecretManagementDelegateServiceImpl.getVaultRestClient;
import static software.wings.service.impl.security.VaultServiceImpl.VAULT_VAILDATION_URL;

import com.google.common.base.Preconditions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Response;
import software.wings.beans.DelegateTask;
import software.wings.beans.KmsConfig;
import software.wings.beans.VaultConfig;
import software.wings.security.EncryptionType;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.security.VaultSecretValue;
import software.wings.service.intfc.security.EncryptionConfig;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.Misc;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by brett on 11/2/17
 */
public abstract class AbstractSecretManagerValidation extends AbstractDelegateValidateTask {
  private static final Logger logger = LoggerFactory.getLogger(AbstractDelegateValidateTask.class);
  public AbstractSecretManagerValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  protected DelegateConnectionResult validateSecretManager() {
    EncryptionConfig encryptionConfig = getEncryptionConfig();
    // local encryption
    if (encryptionConfig == null) {
      return DelegateConnectionResult.builder()
          .criteria("encryption type: " + EncryptionType.LOCAL)
          .validated(true)
          .build();
    }

    Preconditions.checkNotNull(encryptionConfig);
    if (encryptionConfig instanceof KmsConfig) {
      DelegateConnectionResult delegateConnectionResult = DelegateConnectionResult.builder()
                                                              .criteria("https://aws.amazon.com/")
                                                              .validated(connectableHttpUrl("https://aws.amazon.com/"))
                                                              .build();
      if (!delegateConnectionResult.isValidated()) {
        logger.info("Can not reach to aws at https://aws.amazon.com");
      }
      return delegateConnectionResult;
    }
    if (encryptionConfig instanceof VaultConfig) {
      VaultConfig vaultConfig = (VaultConfig) encryptionConfig;
      Call<Void> request = getVaultRestClient(vaultConfig)
                               .writeSecret(String.valueOf(vaultConfig.getAuthToken()),
                                   SettingVariableTypes.VAULT + "/" + VAULT_VAILDATION_URL, SettingVariableTypes.VAULT,
                                   VaultSecretValue.builder().value(VAULT_VAILDATION_URL).build());

      try {
        Response<Void> response = request.execute();
        return DelegateConnectionResult.builder()
            .criteria(vaultConfig.getVaultUrl())
            .validated(response.isSuccessful())
            .build();
      } catch (IOException e) {
        logger.info("Can not reach to vault at {}, reason: {} ", vaultConfig.getVaultUrl(), Misc.getMessage(e), e);
        return DelegateConnectionResult.builder().criteria(vaultConfig.getVaultUrl()).validated(false).build();
      }
    }
    throw new IllegalStateException("Invalid encryptionConfig " + encryptionConfig);
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    DelegateConnectionResult delegateConnectionResult = validateSecretManager();
    if (!delegateConnectionResult.isValidated()) {
      delegateConnectionResult.setCriteria(getCriteria().get(0));
      return singletonList(delegateConnectionResult);
    }
    return super.validate();
  }

  protected EncryptionConfig getEncryptionConfig() {
    for (Object parmeter : getParameters()) {
      if (parmeter instanceof List) {
        List details = (List) parmeter;
        for (Object detail : details) {
          if (detail instanceof EncryptedDataDetail) {
            return ((EncryptedDataDetail) detail).getEncryptionConfig();
          }
        }
      }
    }
    return null;
  }
}
