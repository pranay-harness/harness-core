package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;
import static software.wings.service.impl.security.VaultServiceImpl.VAULT_VAILDATION_URL;

import com.google.common.base.Preconditions;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.kms.model.GenerateDataKeyRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.KmsConfig;
import software.wings.beans.VaultConfig;
import software.wings.helpers.ext.vault.VaultRestClientFactory;
import software.wings.security.EncryptionType;
import software.wings.security.encryption.EncryptedDataDetail;
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
      KmsConfig kmsConfig = (KmsConfig) encryptionConfig;
      AWSKMS kmsClient =
          AWSKMSClientBuilder.standard()
              .withCredentials(new AWSStaticCredentialsProvider(
                  new BasicAWSCredentials(kmsConfig.getAccessKey(), kmsConfig.getSecretKey())))
              .withRegion(kmsConfig.getRegion() == null ? Regions.US_EAST_1 : Regions.fromName(kmsConfig.getRegion()))
              .build();
      GenerateDataKeyRequest dataKeyRequest = new GenerateDataKeyRequest();
      dataKeyRequest.setKeyId(kmsConfig.getKmsArn());
      dataKeyRequest.setKeySpec("AES_128");
      try {
        kmsClient.generateDataKey(dataKeyRequest);
        return DelegateConnectionResult.builder().criteria(kmsConfig.getValidationCriteria()).validated(true).build();
      } catch (Exception e) {
        logger.info("can't reach to kms {} in region {}", kmsConfig.getUuid(), kmsConfig.getRegion());
        return DelegateConnectionResult.builder().criteria(kmsConfig.getValidationCriteria()).validated(false).build();
      }
    }
    if (encryptionConfig instanceof VaultConfig) {
      VaultConfig vaultConfig = (VaultConfig) encryptionConfig;
      try {
        boolean isSuccessful = VaultRestClientFactory.create(vaultConfig)
                                   .writeSecret(String.valueOf(vaultConfig.getAuthToken()), VAULT_VAILDATION_URL,
                                       SettingVariableTypes.VAULT, VAULT_VAILDATION_URL);
        return DelegateConnectionResult.builder().criteria(vaultConfig.getVaultUrl()).validated(isSuccessful).build();
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
