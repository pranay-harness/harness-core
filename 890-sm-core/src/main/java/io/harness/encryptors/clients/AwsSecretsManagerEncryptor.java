package io.harness.encryptors.clients;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.AWS_SECRETS_MANAGER_OPERATION_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.helpers.ext.vault.VaultRestClientFactory.getFullPath;
import static io.harness.threading.Morpheus.sleep;

import static java.time.Duration.ofMillis;

import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.services.secretsmanager.model.AWSSecretsManagerException;
import com.google.common.base.Preconditions;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.UUIDGenerator;
import io.harness.encryptors.VaultEncryptor;
import io.harness.exception.SecretManagementDelegateException;
import io.harness.exception.SecretManagementException;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptedRecordData.EncryptedRecordDataBuilder;
import io.harness.security.encryption.EncryptionConfig;

import software.wings.beans.AwsSecretsManagerConfig;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.CreateSecretRequest;
import com.amazonaws.services.secretsmanager.model.CreateSecretResult;
import com.amazonaws.services.secretsmanager.model.DeleteSecretRequest;
import com.amazonaws.services.secretsmanager.model.DeleteSecretResult;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.amazonaws.services.secretsmanager.model.ResourceNotFoundException;
import com.amazonaws.services.secretsmanager.model.Tag;
import com.amazonaws.services.secretsmanager.model.UpdateSecretRequest;
import com.amazonaws.services.secretsmanager.model.UpdateSecretResult;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.concurrent.TimeUnit;
import javax.validation.executable.ValidateOnExecution;
import lombok.extern.slf4j.Slf4j;

@ValidateOnExecution
@Singleton
@Slf4j
@OwnedBy(PL)
public class AwsSecretsManagerEncryptor implements VaultEncryptor {
  private final TimeLimiter timeLimiter;
  private final int NUM_OF_RETRIES = 3;
  private static final String KEY_SEPARATOR = "#";
  private static final JsonParser JSON_PARSER = new JsonParser();
  private static final String AWS_SECRETS_MANAGER_VALIDATION_URL = "aws_secrets_manager_validation";

  private static class ParsedSecretRef {
    String secretPath;
    String keyName;
  }

  @Inject
  public AwsSecretsManagerEncryptor(TimeLimiter timeLimiter) {
    this.timeLimiter = timeLimiter;
  }

  @Override
  public EncryptedRecord createSecret(
      String accountId, String name, String plaintext, EncryptionConfig encryptionConfig) {
    AwsSecretsManagerConfig awsSecretsManagerConfig = (AwsSecretsManagerConfig) encryptionConfig;
    int failedAttempts = 0;
    while (true) {
      try {
        return timeLimiter.callWithTimeout(
            () -> upsertSecretInternal(name, plaintext, null, awsSecretsManagerConfig), 15, TimeUnit.SECONDS, true);
      } catch (Exception e) {
        failedAttempts++;
        log.warn("encryption failed. trial num: {}", failedAttempts, e);
        if (failedAttempts == NUM_OF_RETRIES) {
          String message = "Secret creation failed after " + NUM_OF_RETRIES + " retries";
          throw new SecretManagementDelegateException(AWS_SECRETS_MANAGER_OPERATION_ERROR, message, e, USER);
        }
        sleep(ofMillis(1000));
      }
    }
  }

  @Override
  public EncryptedRecord updateSecret(String accountId, String name, String plaintext, EncryptedRecord existingRecord,
      EncryptionConfig encryptionConfig) {
    AwsSecretsManagerConfig awsSecretsManagerConfig = (AwsSecretsManagerConfig) encryptionConfig;
    int failedAttempts = 0;
    while (true) {
      try {
        return timeLimiter.callWithTimeout(
            ()
                -> upsertSecretInternal(name, plaintext, existingRecord, awsSecretsManagerConfig),
            10, TimeUnit.SECONDS, true);
      } catch (Exception e) {
        failedAttempts++;
        log.warn("encryption failed. trial num: {}", failedAttempts, e);
        if (failedAttempts == NUM_OF_RETRIES) {
          String message = "Secret update failed after " + NUM_OF_RETRIES + " retries";
          throw new SecretManagementDelegateException(AWS_SECRETS_MANAGER_OPERATION_ERROR, message, e, USER);
        }
        sleep(ofMillis(1000));
      }
    }
  }

  @Override
  public EncryptedRecord renameSecret(
      String accountId, String name, EncryptedRecord existingRecord, EncryptionConfig encryptionConfig) {
    AwsSecretsManagerConfig awsSecretsManagerConfig = (AwsSecretsManagerConfig) encryptionConfig;
    int failedAttempts = 0;
    while (true) {
      try {
        return timeLimiter.callWithTimeout(
            () -> renameSecretInternal(name, existingRecord, awsSecretsManagerConfig), 15, TimeUnit.SECONDS, true);
      } catch (Exception e) {
        failedAttempts++;
        log.warn("encryption failed. trial num: {}", failedAttempts, e);
        if (failedAttempts == NUM_OF_RETRIES) {
          String message = "Secret update failed after " + NUM_OF_RETRIES + " retries";
          throw new SecretManagementDelegateException(AWS_SECRETS_MANAGER_OPERATION_ERROR, message, e, USER);
        }
        sleep(ofMillis(1000));
      }
    }
  }

  @Override
  public boolean deleteSecret(String accountId, EncryptedRecord existingRecord, EncryptionConfig encryptionConfig) {
    long startTime = System.currentTimeMillis();
    AwsSecretsManagerConfig awsSecretsManagerConfig = (AwsSecretsManagerConfig) encryptionConfig;

    AWSSecretsManager client = getAwsSecretsManagerClient(awsSecretsManagerConfig);
    DeleteSecretRequest request =
        new DeleteSecretRequest().withSecretId(existingRecord.getEncryptionKey()).withForceDeleteWithoutRecovery(true);
    DeleteSecretResult result = client.deleteSecret(request);

    log.info("Done deleting AWS secret {} in {}ms", existingRecord.getEncryptionKey(),
        System.currentTimeMillis() - startTime);
    return result != null;
  }

  @Override
  public boolean validateReference(String accountId, String path, EncryptionConfig encryptionConfig) {
    return isNotEmpty(fetchSecretValue(accountId, EncryptedRecordData.builder().path(path).build(), encryptionConfig));
  }

  @Override
  public char[] fetchSecretValue(String accountId, EncryptedRecord encryptedRecord, EncryptionConfig encryptionConfig) {
    if (isEmpty(encryptedRecord.getEncryptionKey()) && isEmpty(encryptedRecord.getPath())) {
      return null;
    }
    AwsSecretsManagerConfig awsSecretsManagerConfig = (AwsSecretsManagerConfig) encryptionConfig;
    int failedAttempts = 0;
    while (true) {
      try {
        return timeLimiter.callWithTimeout(
            () -> fetchSecretValueInternal(encryptedRecord, awsSecretsManagerConfig), 15, TimeUnit.SECONDS, true);
      } catch (Exception e) {
        failedAttempts++;
        log.warn("encryption failed. trial num: {}", failedAttempts, e);
        if (failedAttempts == NUM_OF_RETRIES) {
          String message = "Fetching secret failed after " + NUM_OF_RETRIES + " retries";
          throw new SecretManagementDelegateException(AWS_SECRETS_MANAGER_OPERATION_ERROR, message, e, USER);
        }
        sleep(ofMillis(1000));
      }
    }
  }

  @Override
  public boolean validateSecretManagerConfiguration(String accountId, EncryptionConfig encryptionConfig) {
    AwsSecretsManagerConfig secretsManagerConfig=(AwsSecretsManagerConfig) encryptionConfig;
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
      throw new SecretManagementException(AWS_SECRETS_MANAGER_OPERATION_ERROR, message, e, USER);
    }
    log.info("Test connection to AWS Secrets Manager Succeeded for {}", secretsManagerConfig.getName());
    return true;
  }

  private EncryptedRecord renameSecretInternal(
      String name, EncryptedRecord existingRecord, AwsSecretsManagerConfig secretsManagerConfig) {
    char[] value = fetchSecretValueInternal(existingRecord, secretsManagerConfig);
    if (isEmpty(value)) {
      String message = "Empty value fetched when trying to rename the secret " + existingRecord.getName();
      throw new SecretManagementDelegateException(AWS_SECRETS_MANAGER_OPERATION_ERROR, message, USER);
    }
    return upsertSecretInternal(name, new String(value), existingRecord, secretsManagerConfig);
  }

  private EncryptedRecord upsertSecretInternal(
      String name, String value, EncryptedRecord existingSecret, AwsSecretsManagerConfig secretsManagerConfig) {
    final String fullSecretName = getFullPath(secretsManagerConfig.getSecretNamePrefix(), name);
    long startTime = System.currentTimeMillis();
    log.info("Saving secret '{}' into AWS Secrets Manager: {}", fullSecretName, secretsManagerConfig.getName());
    AWSSecretsManager client = getAwsSecretsManagerClient(secretsManagerConfig);

    boolean secretExists = false;
    try {
      secretExists = isNotEmpty(
          fetchSecretValueInternal(EncryptedRecordData.builder().path(fullSecretName).build(), secretsManagerConfig));
    } catch (ResourceNotFoundException e) {
      // If reaching here, it means the resource doesn't exist.
    }
    EncryptedRecordDataBuilder encryptedRecordDataBuilder = EncryptedRecordData.builder();
    if (!secretExists) {
      // Create the secret with proper tags.
      CreateSecretRequest request = new CreateSecretRequest()
                                        .withName(fullSecretName)
                                        .withSecretString(value)
                                        .withTags(new Tag().withKey("createdBy").withValue("Harness"));
      CreateSecretResult createSecretResult = client.createSecret(request);
      encryptedRecordDataBuilder.encryptionKey(fullSecretName)
          .encryptedValue(createSecretResult.getARN().toCharArray());
    } else {
      // Update the existing secret with new secret value.
      UpdateSecretRequest request = new UpdateSecretRequest().withSecretId(fullSecretName).withSecretString(value);
      UpdateSecretResult updateSecretResult = client.updateSecret(request);
      encryptedRecordDataBuilder.encryptionKey(fullSecretName)
          .encryptedValue(updateSecretResult.getARN().toCharArray());
    }
    if (existingSecret != null) {
      final String oldFullSecretName = existingSecret.getEncryptionKey();
      if (!oldFullSecretName.equals(fullSecretName)) {
        log.info("Old path of the secret {} is different than the current one {}. Deleting the old secret",
            oldFullSecretName, fullSecretName);
        deleteSecret(secretsManagerConfig.getAccountId(), existingSecret, secretsManagerConfig);
      }
    }
    log.info("Done saving secret {} into AWS Secrets Manager in {} ms", fullSecretName,
        System.currentTimeMillis() - startTime);
    return encryptedRecordDataBuilder.build();
  }

  private char[] fetchSecretValueInternal(EncryptedRecord data, AwsSecretsManagerConfig secretsManagerConfig) {
    long startTime = System.currentTimeMillis();

    final String secretName;
    String refKeyName = null;
    if (isNotEmpty(data.getPath())) {
      String path = data.getPath();
      ParsedSecretRef secretRef = parsedSecretRef(path);
      secretName = secretRef.secretPath;
      refKeyName = secretRef.keyName;
    } else {
      secretName = data.getEncryptionKey();
    }

    AWSSecretsManager client = getAwsSecretsManagerClient(secretsManagerConfig);
    GetSecretValueRequest request = new GetSecretValueRequest().withSecretId(secretName);
    GetSecretValueResult result = client.getSecretValue(request);
    String secretValue = result.getSecretString();

    char[] decryptedValue = null;
    if (isNotEmpty(refKeyName)) {
      JsonElement element = JSON_PARSER.parse(secretValue);
      if (element.getAsJsonObject().has(refKeyName)) {
        JsonElement refKeyedElement = element.getAsJsonObject().get(refKeyName);
        decryptedValue = refKeyedElement.getAsString().toCharArray();
      }
    } else {
      decryptedValue = secretValue.toCharArray();
    }

    log.info("Done decrypting AWS secret {} in {}ms", secretName, System.currentTimeMillis() - startTime);
    return decryptedValue;
  }

  @VisibleForTesting
  public AWSSecretsManager getAwsSecretsManagerClient(AwsSecretsManagerConfig secretsManagerConfig) {
    return AWSSecretsManagerClientBuilder.standard()
        .withCredentials(getAwsCredentialsProvider(secretsManagerConfig))
        .withRegion(secretsManagerConfig.getRegion() == null ? Regions.US_EAST_1
                                                             : Regions.fromName(secretsManagerConfig.getRegion()))
        .build();
  }

  public AWSCredentialsProvider getAwsCredentialsProvider(AwsSecretsManagerConfig secretsManagerConfig){
    System.out.println("");
    if(!secretsManagerConfig.isAssumeIamRoleOnDelegate()) {
      Preconditions.checkNotNull(secretsManagerConfig.getAccessKey(), "You must provide an AccessKey if AssumeIAMRole is not selected");
      Preconditions.checkNotNull(secretsManagerConfig.getSecretKey(), "You must provide a SecretKey if AssumeIAMRole is not selected");
      log.info("Instantiating STSAssumeRoleSessionCredentialsProvider with config:" + secretsManagerConfig);
      return new AWSStaticCredentialsProvider(
              new BasicAWSCredentials(secretsManagerConfig.getAccessKey(), secretsManagerConfig.getSecretKey()));
    }else {
      System.out.println("");
      if(secretsManagerConfig.isAssumeStsRoleOnDelegate()){
        log.info("Instantiating STSAssumeRoleSessionCredentialsProvider with config:" + secretsManagerConfig);
        Preconditions.checkNotNull(secretsManagerConfig.getRoleArn(), "You must provide RoleARN if AssumeStsRole is selected");
        STSAssumeRoleSessionCredentialsProvider.Builder sessionCredentialsProviderBuilder=
                new STSAssumeRoleSessionCredentialsProvider.Builder(secretsManagerConfig.getRoleArn(), UUIDGenerator.generateUuid());
        sessionCredentialsProviderBuilder.withRoleSessionDurationSeconds(secretsManagerConfig.getAssumeStsRoleDuration());
        sessionCredentialsProviderBuilder.withExternalId(secretsManagerConfig.getExternalName());
        return sessionCredentialsProviderBuilder.build();
      }else{
        log.info("Instantiating DefaultCredentialProviderChain to resolve credential");
        try {
          return new DefaultAWSCredentialsProviderChain();
        }catch(SdkClientException exception){
          throw new SecretManagementDelegateException(AWS_SECRETS_MANAGER_OPERATION_ERROR,exception.getMessage(),USER_SRE);
        }
      }
    }
  }

  private ParsedSecretRef parsedSecretRef(String path) {
    String[] parts = path.split(KEY_SEPARATOR);
    ParsedSecretRef secretRef = new ParsedSecretRef();
    secretRef.secretPath = parts[0];
    if (parts.length > 1) {
      secretRef.keyName = parts[1];
    }
    return secretRef;
  }
}
