package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import software.wings.beans.DelegateTask;
import software.wings.beans.VaultConfig;
import software.wings.exception.WingsException;
import software.wings.security.EncryptionType;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionConfig;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by brett on 11/2/17
 */
public class SecretManagerValidation extends AbstractDelegateValidateTask {
  public SecretManagerValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(Arrays.stream(getParameters())
                             .filter(o -> o instanceof List)
                             .map(obj -> {
                               List<EncryptedDataDetail> encryptedDataDetails = (List<EncryptedDataDetail>) obj;
                               String secretManagerUrl = null;
                               for (EncryptedDataDetail encryptedDataDetail : encryptedDataDetails) {
                                 EncryptionConfig encryptionConfig = encryptedDataDetail.getEncryptionConfig();
                                 switch (encryptedDataDetail.getEncryptionType()) {
                                   case KMS:
                                     secretManagerUrl = "https://aws.amazon.com/";
                                     break;
                                   case VAULT:
                                     secretManagerUrl = ((VaultConfig) encryptionConfig).getVaultUrl();
                                     break;

                                   default:
                                     throw new WingsException("Invalid type " + encryptionConfig.getEncryptionType());
                                 }
                                 if (encryptionConfig.getEncryptionType() == EncryptionType.VAULT) {
                                   secretManagerUrl = ((VaultConfig) encryptionConfig).getVaultUrl();
                                 }
                               }
                               return secretManagerUrl;
                             })
                             .findFirst()
                             .orElse(null));
  }
}
