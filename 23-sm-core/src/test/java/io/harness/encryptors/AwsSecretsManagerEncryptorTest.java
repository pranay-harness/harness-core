package io.harness.encryptors;

import static io.harness.rule.OwnerRule.UTKARSH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.encryptors.clients.AwsSecretsManagerEncryptor;
import io.harness.exception.SecretManagementDelegateException;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionType;

import software.wings.beans.AwsSecretsManagerConfig;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.model.AWSSecretsManagerException;
import com.amazonaws.services.secretsmanager.model.CreateSecretRequest;
import com.amazonaws.services.secretsmanager.model.CreateSecretResult;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.amazonaws.services.secretsmanager.model.ResourceNotFoundException;
import com.amazonaws.services.secretsmanager.model.Tag;
import com.amazonaws.services.secretsmanager.model.UpdateSecretRequest;
import com.amazonaws.services.secretsmanager.model.UpdateSecretResult;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AwsSecretsManagerEncryptorTest extends CategoryTest {
  private AwsSecretsManagerEncryptor awsSecretsManagerEncryptor;
  private AwsSecretsManagerConfig awsSecretsManagerConfig;
  private AWSSecretsManager awsSecretsManager;

  @Before
  public void setup() {
    awsSecretsManagerEncryptor = spy(new AwsSecretsManagerEncryptor(new SimpleTimeLimiter()));
    awsSecretsManagerConfig = AwsSecretsManagerConfig.builder()
                                  .accountId(UUIDGenerator.generateUuid())
                                  .name(UUIDGenerator.generateUuid())
                                  .uuid(UUIDGenerator.generateUuid())
                                  .encryptionType(EncryptionType.AWS_SECRETS_MANAGER)
                                  .accessKey(UUIDGenerator.generateUuid())
                                  .secretKey(UUIDGenerator.generateUuid())
                                  .region("us-east-1")
                                  .secretNamePrefix(UUIDGenerator.generateUuid())
                                  .isDefault(false)
                                  .build();
    awsSecretsManager = mock(AWSSecretsManager.class);
    when(awsSecretsManagerEncryptor.getAwsSecretsManagerClient(awsSecretsManagerConfig)).thenReturn(awsSecretsManager);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testCreateSecret() {
    String plainTextValue = UUIDGenerator.generateUuid();
    String secretName = UUIDGenerator.generateUuid();
    String fullSecretName = awsSecretsManagerConfig.getSecretNamePrefix() + "/" + secretName;
    // Create the secret with proper tags.
    CreateSecretRequest createSecretRequest = new CreateSecretRequest()
                                                  .withName(fullSecretName)
                                                  .withSecretString(plainTextValue)
                                                  .withTags(new Tag().withKey("createdBy").withValue("Harness"));
    CreateSecretResult createSecretResult =
        new CreateSecretResult().withName(fullSecretName).withARN(UUIDGenerator.generateUuid());
    when(awsSecretsManager.createSecret(createSecretRequest)).thenReturn(createSecretResult);
    GetSecretValueRequest getSecretRequest = new GetSecretValueRequest().withSecretId(fullSecretName);
    when(awsSecretsManager.getSecretValue(getSecretRequest))
        .thenThrow(new ResourceNotFoundException("Secret not found mock exception"));

    EncryptedRecord encryptedRecord = awsSecretsManagerEncryptor.createSecret(
        awsSecretsManagerConfig.getAccountId(), secretName, plainTextValue, awsSecretsManagerConfig);
    assertThat(encryptedRecord.getEncryptedValue()).isEqualTo(createSecretResult.getARN().toCharArray());
    assertThat(encryptedRecord.getEncryptionKey()).isEqualTo(fullSecretName);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testUpdateSecret() {
    String plainTextValue = UUIDGenerator.generateUuid();
    String secretName = UUIDGenerator.generateUuid();
    String fullSecretName = awsSecretsManagerConfig.getSecretNamePrefix() + "/" + secretName;
    // Create the secret with proper tags.
    UpdateSecretRequest updateSecretRequest =
        new UpdateSecretRequest().withSecretId(fullSecretName).withSecretString(plainTextValue);
    UpdateSecretResult updateSecretResult =
        new UpdateSecretResult().withName(fullSecretName).withARN(UUIDGenerator.generateUuid());
    when(awsSecretsManager.updateSecret(updateSecretRequest)).thenReturn(updateSecretResult);
    GetSecretValueRequest getSecretRequest = new GetSecretValueRequest().withSecretId(fullSecretName);
    GetSecretValueResult getSecretValueResult =
        new GetSecretValueResult().withSecretString(UUIDGenerator.generateUuid());
    when(awsSecretsManager.getSecretValue(getSecretRequest)).thenReturn(getSecretValueResult);

    EncryptedRecord oldRecord = EncryptedRecordData.builder()
                                    .name(UUIDGenerator.generateUuid())
                                    .encryptionKey(UUIDGenerator.generateUuid())
                                    .encryptedValue(UUIDGenerator.generateUuid().toCharArray())
                                    .build();

    EncryptedRecord encryptedRecord = awsSecretsManagerEncryptor.updateSecret(
        awsSecretsManagerConfig.getAccountId(), secretName, plainTextValue, oldRecord, awsSecretsManagerConfig);
    assertThat(encryptedRecord.getEncryptedValue()).isEqualTo(updateSecretResult.getARN().toCharArray());
    assertThat(encryptedRecord.getEncryptionKey()).isEqualTo(fullSecretName);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void renameSecret() {
    String secretName = UUIDGenerator.generateUuid();
    String plainTextValue = UUIDGenerator.generateUuid();
    String fullSecretName = awsSecretsManagerConfig.getSecretNamePrefix() + "/" + secretName;
    CreateSecretRequest createSecretRequest = new CreateSecretRequest()
                                                  .withName(fullSecretName)
                                                  .withSecretString(plainTextValue)
                                                  .withTags(new Tag().withKey("createdBy").withValue("Harness"));
    CreateSecretResult createSecretResult =
        new CreateSecretResult().withName(fullSecretName).withARN(UUIDGenerator.generateUuid());
    when(awsSecretsManager.createSecret(createSecretRequest)).thenReturn(createSecretResult);

    GetSecretValueRequest getSecretRequest = new GetSecretValueRequest().withSecretId(fullSecretName);
    when(awsSecretsManager.getSecretValue(getSecretRequest))
        .thenThrow(new ResourceNotFoundException("Secret not found"));

    EncryptedRecord oldRecord = EncryptedRecordData.builder()
                                    .name(UUIDGenerator.generateUuid())
                                    .encryptionKey(UUIDGenerator.generateUuid())
                                    .encryptedValue(UUIDGenerator.generateUuid().toCharArray())
                                    .build();

    String oldFullName = oldRecord.getEncryptionKey();
    GetSecretValueRequest getSecretRequestOld = new GetSecretValueRequest().withSecretId(oldFullName);
    GetSecretValueResult getSecretValueResult = new GetSecretValueResult().withSecretString(plainTextValue);
    when(awsSecretsManager.getSecretValue(getSecretRequestOld)).thenReturn(getSecretValueResult);

    EncryptedRecord encryptedRecord = awsSecretsManagerEncryptor.renameSecret(
        awsSecretsManagerConfig.getAccountId(), secretName, oldRecord, awsSecretsManagerConfig);
    assertThat(encryptedRecord).isNotNull();
    assertThat(encryptedRecord.getEncryptionKey()).isEqualTo(fullSecretName);
    assertThat(encryptedRecord.getEncryptedValue()).isEqualTo(createSecretResult.getARN().toCharArray());
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testCreateSecret_shouldThrowException() {
    String plainTextValue = UUIDGenerator.generateUuid();
    String secretName = UUIDGenerator.generateUuid();

    when(awsSecretsManager.createSecret(any())).thenThrow(new AWSSecretsManagerException("Dummy exception"));
    when(awsSecretsManager.getSecretValue(any()))
        .thenThrow(new ResourceNotFoundException("Secret not found mock exception"));

    try {
      awsSecretsManagerEncryptor.createSecret(
          awsSecretsManagerConfig.getAccountId(), secretName, plainTextValue, awsSecretsManagerConfig);
      fail("Create Secret should have failed");
    } catch (SecretManagementDelegateException e) {
      assertThat(e.getMessage()).isEqualTo("Secret creation failed after 3 retries");
      assertThat(e.getCause()).isOfAnyClassIn(AWSSecretsManagerException.class);
    }
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testUpdateSecret_shouldThrowException() {
    String plainTextValue = UUIDGenerator.generateUuid();
    String secretName = UUIDGenerator.generateUuid();

    when(awsSecretsManager.createSecret(any())).thenThrow(new AWSSecretsManagerException("Dummy exception"));
    when(awsSecretsManager.getSecretValue(any()))
        .thenThrow(new ResourceNotFoundException("Secret not found mock exception"));

    try {
      awsSecretsManagerEncryptor.updateSecret(awsSecretsManagerConfig.getAccountId(), secretName, plainTextValue,
          mock(EncryptedRecord.class), awsSecretsManagerConfig);
      fail("Update Secret should have failed");
    } catch (SecretManagementDelegateException e) {
      assertThat(e.getMessage()).isEqualTo("Secret update failed after 3 retries");
      assertThat(e.getCause()).isOfAnyClassIn(AWSSecretsManagerException.class);
    }
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testRenameSecret_shouldThrowException() {
    String secretName = UUIDGenerator.generateUuid();
    when(awsSecretsManager.getSecretValue(any()))
        .thenThrow(new ResourceNotFoundException("Secret not found mock exception"));
    EncryptedRecord encryptedRecord = EncryptedRecordData.builder()
                                          .encryptedValue(UUIDGenerator.generateUuid().toCharArray())
                                          .encryptionKey(UUIDGenerator.generateUuid())
                                          .name(UUIDGenerator.generateUuid())
                                          .build();
    try {
      awsSecretsManagerEncryptor.renameSecret(
          awsSecretsManagerConfig.getAccountId(), secretName, encryptedRecord, awsSecretsManagerConfig);
      fail("Rename Secret should have failed");
    } catch (SecretManagementDelegateException e) {
      assertThat(e.getMessage()).isEqualTo("Secret update failed after 3 retries");
      assertThat(e.getCause()).isOfAnyClassIn(ResourceNotFoundException.class);
    }
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testFetchSecret() {
    String key = UUIDGenerator.generateUuid();
    String value = UUIDGenerator.generateUuid();
    String fullSecretName = awsSecretsManagerConfig.getSecretNamePrefix() + "/" + UUIDGenerator.generateUuid();
    String fullSecretNameAndKey = fullSecretName + "#" + key;
    String result = "{\"" + key + "\":\"" + value + "\"}";
    GetSecretValueRequest getSecretRequest = new GetSecretValueRequest().withSecretId(fullSecretName);
    GetSecretValueResult getSecretValueResult = new GetSecretValueResult().withSecretString(result);
    when(awsSecretsManager.getSecretValue(getSecretRequest)).thenReturn(getSecretValueResult);
    EncryptedRecord encryptedRecord = EncryptedRecordData.builder().path(fullSecretNameAndKey).build();
    char[] returnedValue = awsSecretsManagerEncryptor.fetchSecretValue(
        awsSecretsManagerConfig.getAccountId(), encryptedRecord, awsSecretsManagerConfig);
    assertThat(returnedValue).isEqualTo(value.toCharArray());
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testFetchSecret_shouldThrowException() {
    String path = UUIDGenerator.generateUuid();
    GetSecretValueRequest getSecretRequest = new GetSecretValueRequest().withSecretId(path);
    when(awsSecretsManager.getSecretValue(getSecretRequest))
        .thenThrow(new ResourceNotFoundException("Secret not found mock exception"));
    EncryptedRecord encryptedRecord = EncryptedRecordData.builder().path(path).build();
    try {
      awsSecretsManagerEncryptor.fetchSecretValue(
          awsSecretsManagerConfig.getAccountId(), encryptedRecord, awsSecretsManagerConfig);
      fail("fetch secret value should throw exception");
    } catch (SecretManagementDelegateException e) {
      assertThat(e.getMessage()).isEqualTo("Fetching secret failed after 3 retries");
      assertThat(e.getCause()).isOfAnyClassIn(ResourceNotFoundException.class);
    }
  }
}
