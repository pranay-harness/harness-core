package software.wings.service.impl.security;

import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.eraro.ErrorCode;
import io.harness.rule.Owner;
import io.harness.security.SimpleEncryption;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionType;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.FeatureName;
import software.wings.beans.GcpKmsConfig;
import software.wings.beans.KmsConfig;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.gcpkms.GcpKmsEncryptDecryptClient;
import software.wings.service.impl.security.kms.KmsEncryptDecryptClient;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.security.GcpSecretsManagerService;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.SecretManager;

public class GlobalEncryptDecryptClientTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;
  @Mock private KmsEncryptDecryptClient kmsEncryptDecryptClient;
  @Mock private GcpKmsEncryptDecryptClient gcpKmsEncryptDecryptClient;
  @Mock private KmsService kmsService;
  @Mock private GcpSecretsManagerService gcpSecretsManagerService;
  @Mock private SecretManager secretManager;
  @Mock private FeatureFlagService featureFlagService;
  @Inject @InjectMocks private GlobalEncryptDecryptClient globalEncryptDecryptClient;

  @Before
  public void setup() throws Exception {
    initMocks(this);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testEncryptWithAwsKms_shouldPass() {
    String accountId = "accountId";
    char[] value = "value".toCharArray();
    KmsConfig kmsConfig = mock(KmsConfig.class);
    when(kmsConfig.getAccountId()).thenReturn(GLOBAL_ACCOUNT_ID);

    EncryptedData encryptedData = EncryptedData.builder().name("Dummy").build();
    when(kmsEncryptDecryptClient.encrypt(accountId, value, kmsConfig)).thenReturn(encryptedData);

    EncryptedData returnedEncryptedData = globalEncryptDecryptClient.encrypt(accountId, value, kmsConfig);
    assertThat(returnedEncryptedData).isNotNull();
    assertThat(returnedEncryptedData.getName()).isEqualTo(encryptedData.getName());

    verify(kmsEncryptDecryptClient, times(1)).encrypt(accountId, value, kmsConfig);
    verify(gcpKmsEncryptDecryptClient, times(0)).encrypt(any(), any(), any(), eq(null));
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testEncryptWithAwsKms_shouldFail() {
    String accountId = "accountId";
    char[] value = "value".toCharArray();
    KmsConfig kmsConfig = mock(KmsConfig.class);
    when(kmsConfig.getAccountId()).thenReturn(accountId);

    EncryptedData encryptedData = null;
    boolean errorCatched = false;
    try {
      encryptedData = globalEncryptDecryptClient.encrypt(accountId, value, kmsConfig);
    } catch (SecretManagementException e) {
      assertThat(e).isNotNull();
      errorCatched = true;
    }
    assertThat(encryptedData).isNull();
    assertThat(errorCatched).isTrue();

    verify(gcpKmsEncryptDecryptClient, times(0)).encrypt(any(), any(), any(), eq(null));
    verify(kmsEncryptDecryptClient, times(0)).encrypt(any(), any(), any());
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testEncryptWithGcpKms_shouldPass() {
    String accountId = "accountId";
    char[] value = "value".toCharArray();

    GcpKmsConfig gcpKmsConfig = mock(GcpKmsConfig.class);
    KmsConfig kmsConfig = mock(KmsConfig.class);
    when(gcpKmsConfig.getAccountId()).thenReturn(GLOBAL_ACCOUNT_ID);

    EncryptedData expectedGCPKMSEncryptedData = EncryptedData.builder()
                                                    .name("gcpName")
                                                    .accountId(accountId)
                                                    .encryptionKey("gcpEncryptionKey")
                                                    .encryptedValue("gcpEncryptionValue".toCharArray())
                                                    .kmsId("gcpKmsId")
                                                    .enabled(true)
                                                    .encryptionType(EncryptionType.GCP_KMS)
                                                    .build();

    EncryptedData expectedAWSKMSEncryptedData = EncryptedData.builder()
                                                    .name("awsName")
                                                    .accountId(accountId)
                                                    .encryptionKey("awsEncryptionKey")
                                                    .encryptedValue("awsEncryptionValue".toCharArray())
                                                    .kmsId("awsKmsId")
                                                    .enabled(true)
                                                    .encryptionType(EncryptionType.KMS)
                                                    .build();

    when(gcpKmsEncryptDecryptClient.encrypt(String.valueOf(value), accountId, gcpKmsConfig, null))
        .thenReturn(expectedGCPKMSEncryptedData);
    when(kmsService.getGlobalKmsConfig()).thenReturn(kmsConfig);
    when(kmsEncryptDecryptClient.encrypt(accountId, value, kmsConfig)).thenReturn(expectedAWSKMSEncryptedData);

    EncryptedData returnedEncryptedData = globalEncryptDecryptClient.encrypt(accountId, value, gcpKmsConfig);

    assertThat(returnedEncryptedData).isNotNull();
    assertThat(returnedEncryptedData.getName()).isEqualTo(expectedGCPKMSEncryptedData.getName());
    assertThat(returnedEncryptedData.getAccountId()).isEqualTo(accountId);
    assertThat(returnedEncryptedData.getEncryptionType()).isEqualTo(expectedGCPKMSEncryptedData.getEncryptionType());
    assertThat(returnedEncryptedData.getEncryptionKey()).isEqualTo(expectedGCPKMSEncryptedData.getEncryptionKey());
    assertThat(returnedEncryptedData.getEncryptedValue()).isEqualTo(expectedGCPKMSEncryptedData.getEncryptedValue());
    assertThat(returnedEncryptedData.getKmsId()).isEqualTo(expectedGCPKMSEncryptedData.getKmsId());
    assertThat(returnedEncryptedData.getBackupEncryptedValue())
        .isEqualTo(expectedAWSKMSEncryptedData.getEncryptedValue());
    assertThat(returnedEncryptedData.getBackupEncryptionKey())
        .isEqualTo(expectedAWSKMSEncryptedData.getEncryptionKey());
    assertThat(returnedEncryptedData.getBackupEncryptionType())
        .isEqualTo(expectedAWSKMSEncryptedData.getEncryptionType());
    assertThat(returnedEncryptedData.getBackupKmsId()).isEqualTo(expectedAWSKMSEncryptedData.getKmsId());

    verify(kmsEncryptDecryptClient, times(1)).encrypt(accountId, value, kmsConfig);
    verify(gcpKmsEncryptDecryptClient, times(1)).encrypt(String.valueOf(value), accountId, gcpKmsConfig, null);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testEncryptWithGcpKms_shouldFail() {
    String accountId = "accountId";
    char[] value = "value".toCharArray();
    GcpKmsConfig gcpKmsConfig = mock(GcpKmsConfig.class);
    when(gcpKmsConfig.getAccountId()).thenReturn(accountId);

    EncryptedData encryptedData = null;
    boolean errorCatched = false;
    try {
      encryptedData = globalEncryptDecryptClient.encrypt(accountId, value, gcpKmsConfig);
    } catch (SecretManagementException e) {
      assertThat(e).isNotNull();
      errorCatched = true;
    }
    assertThat(encryptedData).isNull();
    assertThat(errorCatched).isTrue();
    verify(gcpKmsEncryptDecryptClient, times(0)).encrypt(any(), any(), any(), eq(null));
    verify(kmsEncryptDecryptClient, times(0)).encrypt(any(), any(), any());
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testDecryptWithAwsKms_shouldPass() {
    String accountId = "accountId";
    char[] value = "value".toCharArray();
    KmsConfig kmsConfig = mock(KmsConfig.class);
    GcpKmsConfig gcpKmsConfig = mock(GcpKmsConfig.class);
    when(kmsConfig.getAccountId()).thenReturn(GLOBAL_ACCOUNT_ID);
    when(featureFlagService.isEnabled(FeatureName.SWITCH_GLOBAL_TO_GCP_KMS, accountId)).thenReturn(true);

    EncryptedData awsKmsEncryptedData = EncryptedData.builder()
                                            .name("awsName")
                                            .accountId(accountId)
                                            .encryptionKey("awsEncryptionKey")
                                            .encryptedValue("awsEncryptionValue".toCharArray())
                                            .kmsId("awsKmsId")
                                            .enabled(true)
                                            .encryptionType(EncryptionType.KMS)
                                            .build();

    String encryptedDataId = wingsPersistence.save(awsKmsEncryptedData);
    awsKmsEncryptedData.setUuid(encryptedDataId);

    EncryptedData oldEncryptedData = wingsPersistence.get(EncryptedData.class, encryptedDataId);

    EncryptedData expectedGcpKmsEncryptedData = EncryptedData.builder()
                                                    .name("gcpName")
                                                    .accountId(accountId)
                                                    .encryptionKey("gcpEncryptionKey")
                                                    .encryptedValue("gcpEncryptionValue".toCharArray())
                                                    .kmsId("gcpKmsId")
                                                    .enabled(true)
                                                    .encryptionType(EncryptionType.GCP_KMS)
                                                    .build();

    when(kmsEncryptDecryptClient.decrypt(awsKmsEncryptedData, kmsConfig)).thenReturn(value);
    when(gcpSecretsManagerService.getGlobalKmsConfig()).thenReturn(gcpKmsConfig);
    when(gcpKmsEncryptDecryptClient.encrypt(String.valueOf(value), accountId, gcpKmsConfig, null))
        .thenReturn(expectedGcpKmsEncryptedData);
    when(gcpKmsEncryptDecryptClient.decrypt(awsKmsEncryptedData, gcpKmsConfig)).thenReturn(value);

    char[] returnedValue = globalEncryptDecryptClient.decrypt(awsKmsEncryptedData, accountId, kmsConfig);
    assertThat(returnedValue).isEqualTo(value);

    EncryptedData savedEncryptedData = wingsPersistence.get(EncryptedData.class, encryptedDataId);
    assertThat(savedEncryptedData).isNotNull();
    assertThat(savedEncryptedData.getEncryptedValue()).isEqualTo(expectedGcpKmsEncryptedData.getEncryptedValue());
    assertThat(savedEncryptedData.getEncryptionKey()).isEqualTo(expectedGcpKmsEncryptedData.getEncryptionKey());
    assertThat(savedEncryptedData.getEncryptionType()).isEqualTo(expectedGcpKmsEncryptedData.getEncryptionType());
    assertThat(savedEncryptedData.getKmsId()).isEqualTo(expectedGcpKmsEncryptedData.getKmsId());
    assertThat(savedEncryptedData.getBackupEncryptedValue()).isEqualTo(oldEncryptedData.getEncryptedValue());
    assertThat(savedEncryptedData.getBackupEncryptionType()).isEqualTo(oldEncryptedData.getEncryptionType());
    assertThat(savedEncryptedData.getBackupEncryptionKey()).isEqualTo(oldEncryptedData.getEncryptionKey());
    assertThat(savedEncryptedData.getBackupKmsId()).isEqualTo(oldEncryptedData.getKmsId());

    verify(kmsEncryptDecryptClient, times(1)).decrypt(awsKmsEncryptedData, kmsConfig);
    verify(gcpKmsEncryptDecryptClient, times(1)).encrypt(String.valueOf(value), accountId, gcpKmsConfig, null);
    verify(gcpKmsEncryptDecryptClient, times(1)).decrypt(awsKmsEncryptedData, gcpKmsConfig);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testDecryptWithAwsKms_shouldPass_backupShouldFail() {
    String accountId = "accountId";
    char[] value = "value".toCharArray();
    char[] unexpectedValue = "unexpectedValue".toCharArray();
    KmsConfig kmsConfig = mock(KmsConfig.class);
    GcpKmsConfig gcpKmsConfig = mock(GcpKmsConfig.class);
    when(kmsConfig.getAccountId()).thenReturn(GLOBAL_ACCOUNT_ID);
    when(featureFlagService.isEnabled(FeatureName.SWITCH_GLOBAL_TO_GCP_KMS, accountId)).thenReturn(true);

    EncryptedData awsKmsEncryptedData = EncryptedData.builder()
                                            .name("awsName")
                                            .accountId(accountId)
                                            .encryptionKey("awsEncryptionKey")
                                            .encryptedValue("awsEncryptionValue".toCharArray())
                                            .kmsId("awsKmsId")
                                            .enabled(true)
                                            .encryptionType(EncryptionType.KMS)
                                            .build();

    String encryptedDataId = wingsPersistence.save(awsKmsEncryptedData);
    awsKmsEncryptedData.setUuid(encryptedDataId);

    EncryptedData oldEncryptedData = wingsPersistence.get(EncryptedData.class, encryptedDataId);

    EncryptedData expectedGcpKmsEncryptedData = EncryptedData.builder()
                                                    .name("gcpName")
                                                    .accountId(accountId)
                                                    .encryptionKey("gcpEncryptionKey")
                                                    .encryptedValue("gcpEncryptionValue".toCharArray())
                                                    .kmsId("gcpKmsId")
                                                    .enabled(true)
                                                    .encryptionType(EncryptionType.GCP_KMS)
                                                    .build();

    when(kmsEncryptDecryptClient.decrypt(awsKmsEncryptedData, kmsConfig)).thenReturn(value);
    when(gcpSecretsManagerService.getGlobalKmsConfig()).thenReturn(gcpKmsConfig);
    when(gcpKmsEncryptDecryptClient.encrypt(String.valueOf(value), accountId, gcpKmsConfig, null))
        .thenReturn(expectedGcpKmsEncryptedData);
    when(gcpKmsEncryptDecryptClient.decrypt(awsKmsEncryptedData, gcpKmsConfig)).thenReturn(unexpectedValue);

    char[] returnedValue = globalEncryptDecryptClient.decrypt(awsKmsEncryptedData, accountId, kmsConfig);
    assertThat(returnedValue).isEqualTo(value);

    EncryptedData savedEncryptedData = wingsPersistence.get(EncryptedData.class, encryptedDataId);
    assertThat(savedEncryptedData).isNotNull();
    assertThat(savedEncryptedData.getEncryptedValue()).isEqualTo(oldEncryptedData.getEncryptedValue());
    assertThat(savedEncryptedData.getEncryptionKey()).isEqualTo(oldEncryptedData.getEncryptionKey());
    assertThat(savedEncryptedData.getEncryptionType()).isEqualTo(oldEncryptedData.getEncryptionType());
    assertThat(savedEncryptedData.getKmsId()).isEqualTo(oldEncryptedData.getKmsId());
    assertThat(savedEncryptedData.getBackupEncryptedValue()).isNullOrEmpty();
    assertThat(savedEncryptedData.getBackupEncryptionType()).isNull();
    assertThat(savedEncryptedData.getBackupEncryptionKey()).isNullOrEmpty();
    assertThat(savedEncryptedData.getBackupKmsId()).isNullOrEmpty();

    verify(kmsEncryptDecryptClient, times(1)).decrypt(awsKmsEncryptedData, kmsConfig);
    verify(gcpKmsEncryptDecryptClient, times(1)).encrypt(String.valueOf(value), accountId, gcpKmsConfig, null);
    verify(gcpKmsEncryptDecryptClient, times(1)).decrypt(awsKmsEncryptedData, gcpKmsConfig);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testDecryptWithAwsKms_shouldFail() {
    String accountId = "accountId";
    KmsConfig kmsConfig = mock(KmsConfig.class);
    EncryptedData encryptedData = mock(EncryptedData.class);
    when(kmsConfig.getAccountId()).thenReturn(accountId);

    char[] returnedValue = null;
    boolean errorThrown = false;
    try {
      returnedValue = globalEncryptDecryptClient.decrypt(encryptedData, accountId, kmsConfig);
    } catch (SecretManagementException e) {
      assertThat(e).isNotNull();
      errorThrown = true;
    }

    assertThat(returnedValue).isNull();
    assertThat(errorThrown).isTrue();
    verify(gcpKmsEncryptDecryptClient, times(0)).encrypt(any(), any(), any(), eq(null));
    verify(kmsEncryptDecryptClient, times(0)).encrypt(any(), any(), any());
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testDecryptWithGcpKms_shouldPass_withoutBackup() {
    String accountId = "accountId";
    char[] value = "value".toCharArray();
    GcpKmsConfig gcpKmsConfig = mock(GcpKmsConfig.class);
    when(gcpKmsConfig.getAccountId()).thenReturn(GLOBAL_ACCOUNT_ID);

    EncryptedData gcpKmsEncryptedData = EncryptedData.builder()
                                            .name("gcpName")
                                            .accountId(accountId)
                                            .encryptionKey("gcpEncryptionKey")
                                            .encryptedValue("gcpEncryptionValue".toCharArray())
                                            .kmsId("gcpKmsId")
                                            .enabled(true)
                                            .encryptionType(EncryptionType.GCP_KMS)
                                            .build();
    when(gcpKmsEncryptDecryptClient.decrypt(gcpKmsEncryptedData, gcpKmsConfig)).thenReturn(value);

    char[] receivedValue = globalEncryptDecryptClient.decrypt(gcpKmsEncryptedData, gcpKmsConfig);
    assertThat(receivedValue).isEqualTo(value);
    verify(gcpKmsEncryptDecryptClient, times(1)).decrypt(gcpKmsEncryptedData, gcpKmsConfig);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testDecryptWithGcpKms_shouldPass_usingBackup() {
    String accountId = "accountId";
    char[] value = "value".toCharArray();
    GcpKmsConfig gcpKmsConfig = mock(GcpKmsConfig.class);
    KmsConfig kmsConfig = mock(KmsConfig.class);
    when(gcpKmsConfig.getAccountId()).thenReturn(GLOBAL_ACCOUNT_ID);

    EncryptedData gcpKmsEncryptedData = EncryptedData.builder()
                                            .name("gcpName")
                                            .accountId(accountId)
                                            .encryptionKey("gcpEncryptionKey")
                                            .encryptedValue("gcpEncryptionValue".toCharArray())
                                            .encryptionType(EncryptionType.GCP_KMS)
                                            .kmsId("gcpKmsId")
                                            .backupEncryptionKey("awsEncryptionKey")
                                            .backupEncryptedValue("awsEncryptionKey".toCharArray())
                                            .backupEncryptionType(EncryptionType.KMS)
                                            .backupKmsId("awsKmsId")
                                            .enabled(true)
                                            .build();

    when(gcpKmsEncryptDecryptClient.decrypt(gcpKmsEncryptedData, gcpKmsConfig)).thenThrow(new RuntimeException());
    when(kmsService.getGlobalKmsConfig()).thenReturn(kmsConfig);
    when(kmsEncryptDecryptClient.decrypt(any(EncryptedData.class), eq(kmsConfig))).thenReturn(value);

    char[] receivedValue = globalEncryptDecryptClient.decrypt(gcpKmsEncryptedData, gcpKmsConfig);
    assertThat(receivedValue).isEqualTo(value);

    verify(gcpKmsEncryptDecryptClient, times(1)).decrypt(gcpKmsEncryptedData, gcpKmsConfig);
    ArgumentCaptor<EncryptedData> argumentCaptor = ArgumentCaptor.forClass(EncryptedData.class);
    verify(kmsEncryptDecryptClient, times(1)).decrypt(argumentCaptor.capture(), eq(kmsConfig));
    EncryptedData argumentAwsEncryptedData = argumentCaptor.getValue();
    assertThat(argumentAwsEncryptedData.getEncryptionType()).isEqualTo(gcpKmsEncryptedData.getBackupEncryptionType());
    assertThat(argumentAwsEncryptedData.getEncryptionKey()).isEqualTo(gcpKmsEncryptedData.getBackupEncryptionKey());
    assertThat(argumentAwsEncryptedData.getEncryptedValue()).isEqualTo(gcpKmsEncryptedData.getBackupEncryptedValue());
    assertThat(argumentAwsEncryptedData.getKmsId()).isEqualTo(gcpKmsEncryptedData.getBackupKmsId());
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testDecryptWithGcpKms_shouldFail() {
    String accountId = "accountId";
    GcpKmsConfig gcpKmsConfig = mock(GcpKmsConfig.class);
    EncryptedData encryptedData = mock(EncryptedData.class);
    when(gcpKmsConfig.getAccountId()).thenReturn(accountId);

    char[] returnedValue = null;
    boolean errorThrown = false;
    try {
      returnedValue = globalEncryptDecryptClient.decrypt(encryptedData, gcpKmsConfig);
    } catch (SecretManagementException e) {
      assertThat(e).isNotNull();
      errorThrown = true;
    }

    assertThat(returnedValue).isNull();
    assertThat(errorThrown).isTrue();
    verify(gcpKmsEncryptDecryptClient, times(0)).encrypt(any(), any(), any(), eq(null));
    verify(kmsEncryptDecryptClient, times(0)).encrypt(any(), any(), any());
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testConvertEncryptedRecordToLocallyEncrypted_usingGcpKms() {
    String accountId = "accountId";
    char[] value = "value".toCharArray();
    GcpKmsConfig gcpKmsConfig = mock(GcpKmsConfig.class);
    when(gcpKmsConfig.getAccountId()).thenReturn(GLOBAL_ACCOUNT_ID);
    when(gcpKmsConfig.getEncryptionType()).thenReturn(EncryptionType.GCP_KMS);

    EncryptedData gcpKmsEncryptedData = EncryptedData.builder()
                                            .name("gcpName")
                                            .accountId(accountId)
                                            .encryptionKey("gcpEncryptionKey")
                                            .encryptedValue("gcpEncryptionValue".toCharArray())
                                            .kmsId("gcpKmsId")
                                            .enabled(true)
                                            .path("path")
                                            .encryptionType(EncryptionType.GCP_KMS)
                                            .build();
    gcpKmsEncryptedData.setUuid("uuid");
    when(gcpKmsEncryptDecryptClient.decrypt(gcpKmsEncryptedData, gcpKmsConfig)).thenReturn(value);

    EncryptedRecordData encryptedRecordData = globalEncryptDecryptClient.convertEncryptedRecordToLocallyEncrypted(
        gcpKmsEncryptedData, accountId, gcpKmsConfig);

    assertThat(encryptedRecordData.getEncryptionType()).isEqualTo(EncryptionType.LOCAL);
    char[] returnedValue = new SimpleEncryption(encryptedRecordData.getEncryptionKey())
                               .decryptChars(encryptedRecordData.getEncryptedValue());
    assertThat(returnedValue).isEqualTo(value);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testConvertEncryptedRecordToLocallyEncrypted_usingAwsKms() {
    String accountId = "accountId";
    char[] value = "value".toCharArray();
    KmsConfig kmsConfig = mock(KmsConfig.class);
    GcpKmsConfig gcpKmsConfig = mock(GcpKmsConfig.class);
    when(kmsConfig.getAccountId()).thenReturn(GLOBAL_ACCOUNT_ID);
    when(kmsConfig.getEncryptionType()).thenReturn(EncryptionType.KMS);

    EncryptedData awsKmsEncryptedData = EncryptedData.builder()
                                            .name("awsName")
                                            .accountId(accountId)
                                            .encryptionKey("awsEncryptionKey")
                                            .encryptedValue("awsEncryptionValue".toCharArray())
                                            .kmsId("awsKmsId")
                                            .enabled(true)
                                            .encryptionType(EncryptionType.KMS)
                                            .build();

    EncryptedData expectedGcpKmsEncryptedData = EncryptedData.builder()
                                                    .name("gcpName")
                                                    .accountId(accountId)
                                                    .encryptionKey("gcpEncryptionKey")
                                                    .encryptedValue("gcpEncryptionValue".toCharArray())
                                                    .kmsId("gcpKmsId")
                                                    .enabled(true)
                                                    .encryptionType(EncryptionType.GCP_KMS)
                                                    .build();

    when(kmsEncryptDecryptClient.decrypt(awsKmsEncryptedData, kmsConfig)).thenReturn(value);
    when(gcpSecretsManagerService.getGlobalKmsConfig()).thenReturn(gcpKmsConfig);
    when(gcpKmsEncryptDecryptClient.encrypt(String.valueOf(value), accountId, gcpKmsConfig, null))
        .thenReturn(expectedGcpKmsEncryptedData);
    when(gcpKmsEncryptDecryptClient.decrypt(awsKmsEncryptedData, gcpKmsConfig)).thenReturn(value);

    EncryptedRecordData returnedEncryptedRecord =
        globalEncryptDecryptClient.convertEncryptedRecordToLocallyEncrypted(awsKmsEncryptedData, accountId, kmsConfig);
    assertThat(returnedEncryptedRecord.getEncryptionType()).isEqualTo(EncryptionType.LOCAL);
    char[] returnedValue = new SimpleEncryption(returnedEncryptedRecord.getEncryptionKey())
                               .decryptChars(returnedEncryptedRecord.getEncryptedValue());
    assertThat(returnedValue).isEqualTo(value);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testConvertEncryptedRecordToLocallyEncrypted_usingAwsKms_shouldFail() {
    String accountId = "accountId";
    char[] value = "value".toCharArray();
    KmsConfig kmsConfig = mock(KmsConfig.class);
    when(kmsConfig.getAccountId()).thenReturn(GLOBAL_ACCOUNT_ID);
    when(kmsConfig.getEncryptionType()).thenReturn(EncryptionType.KMS);

    EncryptedData awsKmsEncryptedData = EncryptedData.builder()
                                            .name("awsName")
                                            .accountId(accountId)
                                            .encryptionKey("awsEncryptionKey")
                                            .encryptedValue("awsEncryptionValue".toCharArray())
                                            .kmsId("awsKmsId")
                                            .enabled(true)
                                            .encryptionType(EncryptionType.KMS)
                                            .build();

    when(kmsEncryptDecryptClient.decrypt(awsKmsEncryptedData, kmsConfig))
        .thenThrow(new SecretManagementDelegateException(ErrorCode.GCP_KMS_OPERATION_ERROR, "message", USER_SRE));

    EncryptedRecordData encryptedRecordData =
        globalEncryptDecryptClient.convertEncryptedRecordToLocallyEncrypted(awsKmsEncryptedData, accountId, kmsConfig);
    assertThat(encryptedRecordData).isNotNull();
    assertThat(encryptedRecordData.getEncryptionType()).isEqualTo(awsKmsEncryptedData.getEncryptionType());
  }
}
