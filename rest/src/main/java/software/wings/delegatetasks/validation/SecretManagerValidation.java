package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.VaultConfig;
import software.wings.security.EncryptionType;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionConfig;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by brett on 11/2/17
 */
public class SecretManagerValidation extends AbstractSecretManagerValidation {
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
    return singletonList(super.validateSecretManager());
  }

  @Override
  protected EncryptionConfig getEncryptionConfig() {
    List<EncryptedDataDetail> encryptionDetails = (List<EncryptedDataDetail>) getParameters()[3];
    logger.info("Running validation for task {} for encryptionDetails {}", delegateTaskId, encryptionDetails);
    for (EncryptedDataDetail encryptedDataDetail : encryptionDetails) {
      return encryptedDataDetail.getEncryptionConfig();
    }
    return super.getEncryptionConfig();
  }
}
