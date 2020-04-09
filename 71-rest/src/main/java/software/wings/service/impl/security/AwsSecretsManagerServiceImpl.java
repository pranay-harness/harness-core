package software.wings.service.impl.security;

import static io.harness.data.encoding.EncodingUtils.decodeBase64;
import static io.harness.data.encoding.EncodingUtils.encodeBase64ToByteArray;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.TaskData.DEFAULT_SYNC_CALL_TIMEOUT;
import static io.harness.eraro.ErrorCode.AWS_SECRETS_MANAGER_OPERATION_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.persistence.HPersistence.upToOne;
import static io.harness.security.SimpleEncryption.CHARSET;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.service.intfc.security.SecretManagementDelegateService.NUM_OF_RETRIES;
import static software.wings.service.intfc.security.SecretManager.ACCOUNT_ID_KEY;
import static software.wings.service.intfc.security.SecretManager.SECRET_NAME_KEY;
import static software.wings.settings.SettingValue.SettingVariableTypes.AWS_SECRETS_MANAGER;

import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.AWSSecretsManagerException;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.ResourceNotFoundException;
import com.mongodb.DuplicateKeyException;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptionType;
import io.harness.serializer.KryoUtils;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import software.wings.beans.AwsSecretsManagerConfig;
import software.wings.beans.AwsSecretsManagerConfig.AwsSecretsManagerConfigKeys;
import software.wings.beans.SyncTaskContext;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedData.EncryptedDataKeys;
import software.wings.security.encryption.EncryptedDataParent;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.security.AwsSecretsManagerService;
import software.wings.service.intfc.security.SecretManagementDelegateService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.time.Duration;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * @author marklu on 2019-05-07
 */
@Singleton
@Slf4j
public class AwsSecretsManagerServiceImpl extends AbstractSecretServiceImpl implements AwsSecretsManagerService {
  private static final int AWS_SECRET_CONTENT_SIZE_LIMIT = 7168;
  private static final Pattern AWS_SECRET_NAME_PATTERN = Pattern.compile("^[\\w-/_+=.@!]+$");

  private static final String SECRET_KEY_NAME_SUFFIX = "_secretKey";
  private static final String AWS_SECRETS_MANAGER_VALIDATION_URL = "aws_secrets_manager_validation";

  @Inject private AlertService alertService;
  @Inject private AccountService accountService;

  private void validateSecretName(String name) {
    if (!AWS_SECRET_NAME_PATTERN.matcher(name).find()) {
      throw new WingsException(AWS_SECRETS_MANAGER_OPERATION_ERROR, USER_SRE)
          .addParam(REASON_KEY, "Secret name can only contain alphanumeric characters, or any of: -/_+=.@!");
    }
  }

  @Override
  public EncryptedData encrypt(String name, String value, String accountId, SettingVariableTypes settingType,
      AwsSecretsManagerConfig secretsManagerConfig, EncryptedData encryptedData) {
    // AWS Secrets Manager has restrictions on what the secret name can be. A pre-validation will be helpful.
    validateSecretName(name);

    SyncTaskContext syncTaskContext =
        SyncTaskContext.builder().accountId(accountId).appId(GLOBAL_APP_ID).timeout(DEFAULT_SYNC_CALL_TIMEOUT).build();
    return (EncryptedData) delegateProxyFactory.get(SecretManagementDelegateService.class, syncTaskContext)
        .encrypt(name, value, accountId, settingType, secretsManagerConfig, encryptedData);
  }

  @Override
  public char[] decrypt(EncryptedData data, String accountId, AwsSecretsManagerConfig secretsManagerConfig) {
    // HAR-7605: Shorter timeout for decryption tasks, and it should retry on timeout or failure.
    int failedAttempts = 0;
    while (true) {
      try {
        SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                              .accountId(accountId)
                                              .timeout(Duration.ofSeconds(5).toMillis())
                                              .appId(GLOBAL_APP_ID)
                                              .correlationId(data.getName())
                                              .build();
        return delegateProxyFactory.get(SecretManagementDelegateService.class, syncTaskContext)
            .decrypt(data, secretsManagerConfig);
      } catch (WingsException e) {
        failedAttempts++;
        logger.info("AWS Secrets Manager decryption failed for encryptedData {}. trial num: {}", data.getName(),
            failedAttempts, e);
        if (failedAttempts == NUM_OF_RETRIES) {
          throw e;
        }
        sleep(ofMillis(1000));
      }
    }
  }

  @Override
  public void deleteSecret(String accountId, String path, AwsSecretsManagerConfig secretsManagerConfig) {
    SyncTaskContext syncTaskContext =
        SyncTaskContext.builder().accountId(accountId).appId(GLOBAL_APP_ID).timeout(DEFAULT_SYNC_CALL_TIMEOUT).build();
    delegateProxyFactory.get(SecretManagementDelegateService.class, syncTaskContext)
        .deleteSecret(path, secretsManagerConfig);
  }

  @Override
  public AwsSecretsManagerConfig getAwsSecretsManagerConfig(String accountId, String configId) {
    Query<AwsSecretsManagerConfig> query = wingsPersistence.createQuery(AwsSecretsManagerConfig.class)
                                               .filter(ACCOUNT_ID_KEY, accountId)
                                               .filter(ID_KEY, configId);
    AwsSecretsManagerConfig secretsManagerConfig = query.get();

    if (secretsManagerConfig != null) {
      decryptAsmConfigSecrets(accountId, secretsManagerConfig, false);
    }

    return secretsManagerConfig;
  }

  @Override
  public String saveAwsSecretsManagerConfig(String accountId, AwsSecretsManagerConfig secretsManagerConfig) {
    checkIfSecretsManagerConfigCanBeCreatedOrUpdated(accountId);
    secretsManagerConfig.setAccountId(accountId);

    AwsSecretsManagerConfig oldConfigForAudit = null;
    AwsSecretsManagerConfig savedSecretsManagerConfig = null;
    boolean credentialChanged = true;
    if (!isEmpty(secretsManagerConfig.getUuid())) {
      savedSecretsManagerConfig = getAwsSecretsManagerConfig(accountId, secretsManagerConfig.getUuid());
      if (SECRET_MASK.equals(secretsManagerConfig.getSecretKey())) {
        secretsManagerConfig.setSecretKey(savedSecretsManagerConfig.getSecretKey());
      }
      credentialChanged = !Objects.equals(secretsManagerConfig.getAccessKey(), savedSecretsManagerConfig.getAccessKey())
          || !Objects.equals(secretsManagerConfig.getSecretKey(), savedSecretsManagerConfig.getSecretKey());

      // secret field un-decrypted version of saved AWS config
      savedSecretsManagerConfig = wingsPersistence.get(AwsSecretsManagerConfig.class, secretsManagerConfig.getUuid());
      oldConfigForAudit = KryoUtils.clone(savedSecretsManagerConfig);
    }

    // Validate every time when secret manager config change submitted
    validateSecretsManagerConfig(secretsManagerConfig);

    if (!credentialChanged) {
      // update without access/secret key changes
      savedSecretsManagerConfig.setName(secretsManagerConfig.getName());
      savedSecretsManagerConfig.setDefault(secretsManagerConfig.isDefault());
      savedSecretsManagerConfig.setSecretNamePrefix(secretsManagerConfig.getSecretNamePrefix());

      // PL-3237: Audit secret manager config changes.
      generateAuditForSecretManager(accountId, oldConfigForAudit, savedSecretsManagerConfig);

      return secretManagerConfigService.save(savedSecretsManagerConfig);
    }

    EncryptedData secretKeyEncryptedData = getEncryptedDataForSecretField(
        savedSecretsManagerConfig, secretsManagerConfig, secretsManagerConfig.getSecretKey(), SECRET_KEY_NAME_SUFFIX);

    secretsManagerConfig.setSecretKey(null);
    String secretsManagerConfigId;
    try {
      secretsManagerConfigId = secretManagerConfigService.save(secretsManagerConfig);
    } catch (DuplicateKeyException e) {
      throw new WingsException(AWS_SECRETS_MANAGER_OPERATION_ERROR, USER_SRE)
          .addParam(REASON_KEY, "Another AWS Secrets Manager configuration with the same name or URL exists");
    }

    // Create a LOCAL encrypted record for AWS secret key
    String secretKeyEncryptedDataId = saveSecretField(secretsManagerConfig, secretsManagerConfigId,
        secretKeyEncryptedData, SECRET_KEY_NAME_SUFFIX, AwsSecretsManagerConfigKeys.secretKey);
    secretsManagerConfig.setSecretKey(secretKeyEncryptedDataId);
    // PL-3237: Audit secret manager config changes.
    generateAuditForSecretManager(accountId, oldConfigForAudit, secretsManagerConfig);

    return secretManagerConfigService.save(secretsManagerConfig);
  }

  @Override
  public void validateSecretsManagerConfig(AwsSecretsManagerConfig secretsManagerConfig) {
    try {
      AWSSecretsManager client =
          AWSSecretsManagerClientBuilder.standard()
              .withCredentials(new AWSStaticCredentialsProvider(
                  new BasicAWSCredentials(secretsManagerConfig.getAccessKey(), secretsManagerConfig.getSecretKey())))
              .withRegion(secretsManagerConfig.getRegion() == null ? Regions.US_EAST_1
                                                                   : Regions.fromName(secretsManagerConfig.getRegion()))
              .build();
      GetSecretValueRequest request =
          new GetSecretValueRequest().withSecretId(AWS_SECRETS_MANAGER_VALIDATION_URL + System.currentTimeMillis());
      client.getSecretValue(request);
    } catch (ResourceNotFoundException e) {
      // this exception is expected. It means the credentials are correct, but can't find the resource
      // which means the connectivity to AWS Secrets Manger is ok.
    } catch (AWSSecretsManagerException e) {
      String message =
          "Was not able to reach AWS Secrets Manager using given credentials. Please check your credentials and try again";
      throw new WingsException(AWS_SECRETS_MANAGER_OPERATION_ERROR, message, USER, e).addParam(REASON_KEY, message);
    }
    logger.info("Test connection to AWS Secrets Manager Succeeded for {}", secretsManagerConfig.getName());
  }

  @Override
  public void decryptAsmConfigSecrets(
      String accountId, AwsSecretsManagerConfig secretsManagerConfig, boolean maskSecret) {
    if (maskSecret) {
      secretsManagerConfig.maskSecrets();
    } else {
      EncryptedData encryptedSecretKey = wingsPersistence.get(EncryptedData.class, secretsManagerConfig.getSecretKey());
      checkNotNull(
          encryptedSecretKey, "Secret key can't be null for AWS Secrets Manager " + secretsManagerConfig.getUuid());
      secretsManagerConfig.setSecretKey(String.valueOf(decryptLocal(encryptedSecretKey)));
    }
  }

  private EncryptedData getEncryptedDataForSecretField(AwsSecretsManagerConfig savedSecretsManagerConfig,
      AwsSecretsManagerConfig secretsManagerConfig, String secretValue, String secretNameSuffix) {
    EncryptedData encryptedData = isNotEmpty(secretValue) ? encryptLocal(secretValue.toCharArray()) : null;
    if (savedSecretsManagerConfig != null && encryptedData != null) {
      // Get by auth token encrypted record by Id or name.
      Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class);
      query.criteria(ACCOUNT_ID_KEY)
          .equal(secretsManagerConfig.getAccountId())
          .or(query.criteria(ID_KEY).equal(secretsManagerConfig.getSecretKey()),
              query.criteria(SECRET_NAME_KEY).equal(secretsManagerConfig.getName() + secretNameSuffix));
      EncryptedData savedEncryptedData = query.get();
      if (savedEncryptedData != null) {
        savedEncryptedData.setEncryptionKey(encryptedData.getEncryptionKey());
        savedEncryptedData.setEncryptedValue(encryptedData.getEncryptedValue());
        encryptedData = savedEncryptedData;
      }
    }
    return encryptedData;
  }

  private String saveSecretField(AwsSecretsManagerConfig secretsManagerConfig, String configId,
      EncryptedData secretFieldEncryptedData, String secretNameSuffix, String fieldName) {
    String secretFieldEncryptedDataId = null;
    if (secretFieldEncryptedData != null) {
      secretFieldEncryptedData.setAccountId(secretsManagerConfig.getAccountId());
      secretFieldEncryptedData.addParent(
          EncryptedDataParent.createParentRef(configId, AwsSecretsManagerConfig.class, fieldName, AWS_SECRETS_MANAGER));
      secretFieldEncryptedData.setType(AWS_SECRETS_MANAGER);
      secretFieldEncryptedData.setName(secretsManagerConfig.getName() + secretNameSuffix);
      secretFieldEncryptedDataId = wingsPersistence.save(secretFieldEncryptedData);
    }
    return secretFieldEncryptedDataId;
  }

  @Override
  public boolean deleteAwsSecretsManagerConfig(String accountId, String configId) {
    long count = wingsPersistence.createQuery(EncryptedData.class)
                     .filter(ACCOUNT_ID_KEY, accountId)
                     .filter(EncryptedDataKeys.kmsId, configId)
                     .filter(EncryptedDataKeys.encryptionType, EncryptionType.AWS_SECRETS_MANAGER)
                     .count(upToOne);

    if (count > 0) {
      String message =
          "Can not delete the AWS Secrets Manager configuration since there are secrets encrypted with this. "
          + "Please transition your secrets to another secret manager and try again.";
      throw new WingsException(AWS_SECRETS_MANAGER_OPERATION_ERROR, USER).addParam(REASON_KEY, message);
    }

    AwsSecretsManagerConfig secretsManagerConfig = wingsPersistence.get(AwsSecretsManagerConfig.class, configId);
    checkNotNull(secretsManagerConfig, "No Aws Secrets Manager configuration found with id " + configId);

    if (isNotEmpty(secretsManagerConfig.getSecretKey())) {
      wingsPersistence.delete(EncryptedData.class, secretsManagerConfig.getSecretKey());
      logger.info("Deleted encrypted auth token record {} associated with Aws Secrets Manager '{}'",
          secretsManagerConfig.getSecretKey(), secretsManagerConfig.getName());
    }

    return deleteSecretManagerAndGenerateAudit(accountId, secretsManagerConfig);
  }

  @Override
  public EncryptedData encryptFile(String accountId, AwsSecretsManagerConfig secretsManagerConfig, String name,
      byte[] inputBytes, EncryptedData savedEncryptedData) {
    checkNotNull(secretsManagerConfig, "AWS Secrets Manager configuration can't be null");
    byte[] bytes = encodeBase64ToByteArray(inputBytes);
    if (bytes.length > AWS_SECRET_CONTENT_SIZE_LIMIT) {
      String message = "AWS Secrets Manager limits secret value to " + AWS_SECRET_CONTENT_SIZE_LIMIT + " bytes.";
      throw new SecretManagementException(AWS_SECRETS_MANAGER_OPERATION_ERROR, message, USER_SRE);
    }

    EncryptedData fileData = encrypt(name, new String(CHARSET.decode(ByteBuffer.wrap(bytes)).array()), accountId,
        SettingVariableTypes.CONFIG_FILE, secretsManagerConfig, savedEncryptedData);
    fileData.setAccountId(accountId);
    fileData.setName(name);
    fileData.setType(SettingVariableTypes.CONFIG_FILE);
    fileData.setBase64Encoded(true);
    fileData.setFileSize(inputBytes.length);
    return fileData;
  }

  @Override
  public File decryptFile(File file, String accountId, EncryptedData encryptedData) {
    try {
      AwsSecretsManagerConfig secretsManagerConfig = getAwsSecretsManagerConfig(accountId, encryptedData.getKmsId());
      checkNotNull(secretsManagerConfig, "AWS Secrets Manager configuration can't be null");
      checkNotNull(encryptedData, "Encrypted data record can't be null");
      char[] decrypt = decrypt(encryptedData, accountId, secretsManagerConfig);
      byte[] fileData =
          encryptedData.isBase64Encoded() ? decodeBase64(decrypt) : CHARSET.encode(CharBuffer.wrap(decrypt)).array();
      Files.write(fileData, file);
      return file;
    } catch (IOException ioe) {
      throw new SecretManagementException(
          AWS_SECRETS_MANAGER_OPERATION_ERROR, "Failed to decrypt data into an output file", ioe, USER);
    }
  }

  @Override
  public void decryptToStream(String accountId, EncryptedData encryptedData, OutputStream output) {
    try {
      AwsSecretsManagerConfig secretsManagerConfig = getAwsSecretsManagerConfig(accountId, encryptedData.getKmsId());
      checkNotNull(secretsManagerConfig, "AWS Secrets Manager configuration can't be null");
      checkNotNull(encryptedData, "Encrypted data record can't be null");
      char[] decrypt = decrypt(encryptedData, accountId, secretsManagerConfig);
      byte[] fileData =
          encryptedData.isBase64Encoded() ? decodeBase64(decrypt) : CHARSET.encode(CharBuffer.wrap(decrypt)).array();
      output.write(fileData, 0, fileData.length);
      output.flush();
    } catch (IOException ioe) {
      throw new SecretManagementException(
          AWS_SECRETS_MANAGER_OPERATION_ERROR, "Failed to decrypt data into an output stream", ioe, USER);
    }
  }
}