package software.wings.security.encryption.migration;

import static io.harness.mongo.iterator.MongoPersistenceIterator.MongoPersistenceIteratorBuilder;
import static io.harness.rule.OwnerRule.VIKAS;
import static io.harness.security.encryption.EncryptionType.GCP_KMS;
import static io.harness.security.encryption.EncryptionType.KMS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.FeatureName.ACTIVE_MIGRATION_FROM_AWS_TO_GCP_KMS;
import static software.wings.security.encryption.EncryptedData.EncryptedDataKeys;
import static software.wings.security.encryption.migration.EncryptedDataAwsToGcpKmsMigrationHandler.MAX_RETRY_COUNT;

import com.google.inject.Inject;

import groovy.util.logging.Slf4j;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptionType;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import software.wings.WingsBaseTest;
import software.wings.beans.FeatureFlag;
import software.wings.beans.GcpKmsConfig;
import software.wings.beans.KmsConfig;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.GlobalEncryptDecryptClient;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.security.GcpSecretsManagerService;
import software.wings.service.intfc.security.KmsService;
import software.wings.settings.SettingValue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PersistenceIteratorFactory.class})
@PowerMockIgnore({"javax.security.*", "javax.net.*"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class EncryptedDataAwsToGcpKmsMigrationHandlerTest extends WingsBaseTest {
  @Inject WingsPersistence wingsPersistence;
  @Mock FeatureFlagService featureFlagService;
  @Mock GlobalEncryptDecryptClient globalEncryptDecryptClient;
  @Mock KmsService kmsService;
  @Mock GcpSecretsManagerService gcpSecretsManagerService;
  @Mock KmsConfig awsKmsConfig;
  @Mock GcpKmsConfig gcpKmsConfig;
  @Mock FileService fileService;
  @Mock PersistenceIteratorFactory persistenceIteratorFactory;
  ArgumentCaptor<MongoPersistenceIteratorBuilder> captor =
      ArgumentCaptor.forClass(MongoPersistenceIteratorBuilder.class);
  @Inject @InjectMocks EncryptedDataAwsToGcpKmsMigrationHandler encryptedDataAwsToGcpKmsMigrationHandler;
  static final String AWS_KMS_UUID = UUIDGenerator.generateUuid();
  static final String GCP_KMS_UUID = UUIDGenerator.generateUuid();
  static final String TEST_ENCRYPTION_KEY = "testKey";

  @Before
  public void setup() {
    when(kmsService.getGlobalKmsConfig()).thenReturn(awsKmsConfig);
    when(gcpSecretsManagerService.getGlobalKmsConfig()).thenReturn(gcpKmsConfig);
    when(persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(any(), any(), any()))
        .thenAnswer(
            invocationOnMock -> invocationOnMock.getArgumentAt(2, MongoPersistenceIteratorBuilder.class).build());
    when(awsKmsConfig.getUuid()).thenReturn(AWS_KMS_UUID);
    when(gcpKmsConfig.getUuid()).thenReturn(GCP_KMS_UUID);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testRegisterIterators__WhenAwsKmsConfigIsNull() {
    when(kmsService.getGlobalKmsConfig()).thenReturn(null);
    encryptedDataAwsToGcpKmsMigrationHandler.registerIterators();
    verify(kmsService, times(1)).getGlobalKmsConfig();
    verify(gcpSecretsManagerService, times(0)).getGlobalKmsConfig();
    verify(featureFlagService, times(0)).getFeatureFlag(ACTIVE_MIGRATION_FROM_AWS_TO_GCP_KMS);
    verify(persistenceIteratorFactory, times(0))
        .createPumpIteratorWithDedicatedThreadPool(any(), eq(EncryptedData.class), captor.capture());
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testRegisterIterators__WhenGcpKmsConfigIsNull() {
    when(kmsService.getGlobalKmsConfig()).thenReturn(awsKmsConfig);
    when(gcpSecretsManagerService.getGlobalKmsConfig()).thenReturn(null);
    encryptedDataAwsToGcpKmsMigrationHandler.registerIterators();
    verify(kmsService, times(1)).getGlobalKmsConfig();
    verify(gcpSecretsManagerService, times(1)).getGlobalKmsConfig();
    verify(featureFlagService, times(0)).getFeatureFlag(ACTIVE_MIGRATION_FROM_AWS_TO_GCP_KMS);
    verify(persistenceIteratorFactory, times(0))
        .createPumpIteratorWithDedicatedThreadPool(any(), eq(EncryptedData.class), captor.capture());
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testRegisterIterators__WhenFlagNotPresentInDB() {
    when(kmsService.getGlobalKmsConfig()).thenReturn(awsKmsConfig);
    when(gcpSecretsManagerService.getGlobalKmsConfig()).thenReturn(gcpKmsConfig);
    when(featureFlagService.getFeatureFlag(ACTIVE_MIGRATION_FROM_AWS_TO_GCP_KMS)).thenReturn(Optional.empty());
    encryptedDataAwsToGcpKmsMigrationHandler.registerIterators();
    verify(kmsService, times(1)).getGlobalKmsConfig();
    verify(gcpSecretsManagerService, times(1)).getGlobalKmsConfig();
    verify(featureFlagService, times(1)).getFeatureFlag(ACTIVE_MIGRATION_FROM_AWS_TO_GCP_KMS);
    verify(persistenceIteratorFactory, times(0))
        .createPumpIteratorWithDedicatedThreadPool(any(), eq(EncryptedData.class), captor.capture());
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testRegisterIterators__WhenFlagNotEnabled() {
    when(kmsService.getGlobalKmsConfig()).thenReturn(awsKmsConfig);
    when(gcpSecretsManagerService.getGlobalKmsConfig()).thenReturn(gcpKmsConfig);
    when(featureFlagService.getFeatureFlag(ACTIVE_MIGRATION_FROM_AWS_TO_GCP_KMS))
        .thenReturn(Optional.of(FeatureFlag.builder().build()));
    encryptedDataAwsToGcpKmsMigrationHandler.registerIterators();
    verify(kmsService, times(1)).getGlobalKmsConfig();
    verify(gcpSecretsManagerService, times(1)).getGlobalKmsConfig();
    verify(featureFlagService, times(1)).getFeatureFlag(ACTIVE_MIGRATION_FROM_AWS_TO_GCP_KMS);
    verify(persistenceIteratorFactory, times(0))
        .createPumpIteratorWithDedicatedThreadPool(any(), eq(EncryptedData.class), captor.capture());
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testRegisterIterators__WhenFlagEnabledGlobally() throws NoSuchFieldException, IllegalAccessException {
    when(kmsService.getGlobalKmsConfig()).thenReturn(awsKmsConfig);
    when(gcpSecretsManagerService.getGlobalKmsConfig()).thenReturn(gcpKmsConfig);
    when(featureFlagService.getFeatureFlag(ACTIVE_MIGRATION_FROM_AWS_TO_GCP_KMS))
        .thenReturn(Optional.of(FeatureFlag.builder().enabled(true).build()));
    encryptedDataAwsToGcpKmsMigrationHandler.registerIterators();
    verify(kmsService, times(1)).getGlobalKmsConfig();
    verify(gcpSecretsManagerService, times(1)).getGlobalKmsConfig();
    verify(featureFlagService, times(1)).getFeatureFlag(ACTIVE_MIGRATION_FROM_AWS_TO_GCP_KMS);
    verify(persistenceIteratorFactory, times(1))
        .createPumpIteratorWithDedicatedThreadPool(any(), eq(EncryptedData.class), captor.capture());
    MongoPersistenceIterator<EncryptedData> persistenceIterator =
        (MongoPersistenceIterator<EncryptedData>) captor.getValue().build();
    assertThat(persistenceIterator).isNotNull();

    Field f = persistenceIterator.getClass().getDeclaredField("fieldName");
    f.setAccessible(true);
    String fieldName = (String) f.get(persistenceIterator);
    assertThat(fieldName).isEqualTo(EncryptedDataKeys.nextAwsToGcpKmsMigrationIteration);

    f = persistenceIterator.getClass().getDeclaredField("filterExpander");
    f.setAccessible(true);
    MongoPersistenceIterator.FilterExpander<EncryptedData> filterExpander =
        (MongoPersistenceIterator.FilterExpander<EncryptedData>) f.get(persistenceIterator);
    assertThat(filterExpander).isNotNull();
    final Query<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class).order(Sort.ascending(fieldName));
    filterExpander.filter(query);
    assertThat(query.toString()).contains("accountId\": {\"$exists\": true}");
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testRegisterIterators__WhenFlagEnabledForFewAccounts()
      throws NoSuchFieldException, IllegalAccessException {
    when(kmsService.getGlobalKmsConfig()).thenReturn(awsKmsConfig);
    when(gcpSecretsManagerService.getGlobalKmsConfig()).thenReturn(gcpKmsConfig);
    String testAccount1 = "testAccount1";
    String testAccount2 = "testAccount2";
    Set<String> accountIds = Stream.of(testAccount1, testAccount2).collect(Collectors.toCollection(HashSet::new));
    when(featureFlagService.getFeatureFlag(ACTIVE_MIGRATION_FROM_AWS_TO_GCP_KMS))
        .thenReturn(Optional.of(FeatureFlag.builder().accountIds(accountIds).build()));
    encryptedDataAwsToGcpKmsMigrationHandler.registerIterators();
    verify(featureFlagService, times(1)).getFeatureFlag(ACTIVE_MIGRATION_FROM_AWS_TO_GCP_KMS);
    verify(kmsService, times(1)).getGlobalKmsConfig();
    verify(gcpSecretsManagerService, times(1)).getGlobalKmsConfig();
    verify(persistenceIteratorFactory, times(1))
        .createPumpIteratorWithDedicatedThreadPool(any(), eq(EncryptedData.class), captor.capture());
    MongoPersistenceIterator<EncryptedData> persistenceIterator =
        (MongoPersistenceIterator<EncryptedData>) captor.getValue().build();
    assertThat(persistenceIterator).isNotNull();

    Field f = persistenceIterator.getClass().getDeclaredField("fieldName");
    f.setAccessible(true);
    String fieldName = (String) f.get(persistenceIterator);
    assertThat(fieldName).isEqualTo(EncryptedDataKeys.nextAwsToGcpKmsMigrationIteration);

    f = persistenceIterator.getClass().getDeclaredField("filterExpander");
    f.setAccessible(true);
    MongoPersistenceIterator.FilterExpander<EncryptedData> filterExpander =
        (MongoPersistenceIterator.FilterExpander<EncryptedData>) f.get(persistenceIterator);
    assertThat(filterExpander).isNotNull();
    final Query<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class).order(Sort.ascending(fieldName));
    filterExpander.filter(query);
    String queryString = query.toString();
    assertThat(queryString).contains("accountId\": {\"$in\": [\"");
    assertThat(queryString).contains(testAccount1);
    assertThat(queryString).contains(testAccount2);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testHandleForEncryptedValueAsNull() {
    EncryptedData encryptedData =
        getEncryptedData(TEST_ENCRYPTION_KEY, null, KMS, AWS_KMS_UUID, SettingValue.SettingVariableTypes.SECRET_TEXT);
    String encryptedDataId = wingsPersistence.save(encryptedData);

    EncryptedData oldEncryptedDataInDB = wingsPersistence.get(EncryptedData.class, encryptedDataId);

    when(featureFlagService.isEnabled(ACTIVE_MIGRATION_FROM_AWS_TO_GCP_KMS, oldEncryptedDataInDB.getAccountId()))
        .thenReturn(true);
    encryptedDataAwsToGcpKmsMigrationHandler.handle(oldEncryptedDataInDB);

    EncryptedData updatedEncryptedDataInDB = wingsPersistence.get(EncryptedData.class, encryptedDataId);
    assertThat(updatedEncryptedDataInDB).isNotNull();
    assertThat(updatedEncryptedDataInDB.getEncryptionKey()).isEqualTo(oldEncryptedDataInDB.getEncryptionKey());
    assertThat(updatedEncryptedDataInDB.getEncryptionType()).isEqualTo(GCP_KMS);
    assertThat(updatedEncryptedDataInDB.getKmsId()).isEqualTo(GCP_KMS_UUID);
    assertThat(updatedEncryptedDataInDB.getBackupEncryptionType()).isEqualTo(oldEncryptedDataInDB.getEncryptionType());
    assertThat(updatedEncryptedDataInDB.getBackupEncryptionKey()).isEqualTo(oldEncryptedDataInDB.getEncryptionKey());
    assertThat(updatedEncryptedDataInDB.getBackupKmsId()).isEqualTo(oldEncryptedDataInDB.getKmsId());
    assertThat(updatedEncryptedDataInDB.getEncryptionKey()).isEqualTo(oldEncryptedDataInDB.getEncryptionKey());
    assertThat(updatedEncryptedDataInDB.getLastUpdatedAt()).isGreaterThan(oldEncryptedDataInDB.getLastUpdatedAt());
    assertThat(updatedEncryptedDataInDB.getEncryptedValue()).isNull();
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testHandleWhenFFisDisabled() {
    EncryptedData encryptedData = getEncryptedData(TEST_ENCRYPTION_KEY, TEST_ENCRYPTION_KEY.toCharArray(), KMS,
        AWS_KMS_UUID, SettingValue.SettingVariableTypes.SECRET_TEXT);
    String encryptedDataId = wingsPersistence.save(encryptedData);
    EncryptedData oldEncryptedDataInDB = wingsPersistence.get(EncryptedData.class, encryptedDataId);

    when(featureFlagService.isEnabled(ACTIVE_MIGRATION_FROM_AWS_TO_GCP_KMS, oldEncryptedDataInDB.getAccountId()))
        .thenReturn(false);
    encryptedDataAwsToGcpKmsMigrationHandler.handle(oldEncryptedDataInDB);
    EncryptedData updatedEncryptedDataInDB = wingsPersistence.get(EncryptedData.class, encryptedDataId);
    assertThatOldAndUpdatedRecordAreSame(oldEncryptedDataInDB, updatedEncryptedDataInDB);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testHandleWhenEncryptedValueIsNotNull_And_DecryptionFailed() {
    EncryptedData encryptedData = getEncryptedData(TEST_ENCRYPTION_KEY, TEST_ENCRYPTION_KEY.toCharArray(), KMS,
        AWS_KMS_UUID, SettingValue.SettingVariableTypes.SECRET_TEXT);
    String encryptedDataId = wingsPersistence.save(encryptedData);
    EncryptedData oldEncryptedDataInDB = wingsPersistence.get(EncryptedData.class, encryptedDataId);

    when(featureFlagService.isEnabled(ACTIVE_MIGRATION_FROM_AWS_TO_GCP_KMS, oldEncryptedDataInDB.getAccountId()))
        .thenReturn(true);
    when(globalEncryptDecryptClient.decrypt(oldEncryptedDataInDB, oldEncryptedDataInDB.getAccountId(), awsKmsConfig))
        .thenReturn(null);
    encryptedDataAwsToGcpKmsMigrationHandler.handle(oldEncryptedDataInDB);
    EncryptedData updatedEncryptedDataInDB = wingsPersistence.get(EncryptedData.class, encryptedDataId);
    assertThatOldAndUpdatedRecordAreSame(oldEncryptedDataInDB, updatedEncryptedDataInDB);
    verify(globalEncryptDecryptClient, times(MAX_RETRY_COUNT))
        .decrypt(oldEncryptedDataInDB, oldEncryptedDataInDB.getAccountId(), awsKmsConfig);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testHandleWhenEncryptedValueIsNotNull_And_DecryptionWasSuccessful() {
    EncryptedData encryptedData = getEncryptedData(TEST_ENCRYPTION_KEY, TEST_ENCRYPTION_KEY.toCharArray(), KMS,
        AWS_KMS_UUID, SettingValue.SettingVariableTypes.SECRET_TEXT);
    String encryptedDataId = wingsPersistence.save(encryptedData);
    EncryptedData oldEncryptedDataInDB = wingsPersistence.get(EncryptedData.class, encryptedDataId);

    when(featureFlagService.isEnabled(ACTIVE_MIGRATION_FROM_AWS_TO_GCP_KMS, oldEncryptedDataInDB.getAccountId()))
        .thenReturn(true);
    when(globalEncryptDecryptClient.decrypt(oldEncryptedDataInDB, oldEncryptedDataInDB.getAccountId(), awsKmsConfig))
        .thenReturn(TEST_ENCRYPTION_KEY.toCharArray());
    encryptedDataAwsToGcpKmsMigrationHandler.handle(oldEncryptedDataInDB);
    verify(globalEncryptDecryptClient, atMost(MAX_RETRY_COUNT))
        .decrypt(oldEncryptedDataInDB, oldEncryptedDataInDB.getAccountId(), awsKmsConfig);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testHandleWhenEncryptedValueIsNotNull_And_SecretIsConfigFile() {
    EncryptedData encryptedData = getEncryptedData(TEST_ENCRYPTION_KEY, TEST_ENCRYPTION_KEY.toCharArray(), KMS,
        AWS_KMS_UUID, SettingValue.SettingVariableTypes.CONFIG_FILE);
    String encryptedDataId = wingsPersistence.save(encryptedData);
    EncryptedData oldEncryptedDataInDB = wingsPersistence.get(EncryptedData.class, encryptedDataId);

    when(featureFlagService.isEnabled(ACTIVE_MIGRATION_FROM_AWS_TO_GCP_KMS, oldEncryptedDataInDB.getAccountId()))
        .thenReturn(true);

    when(fileService.download(any(String.class), any(File.class), any(FileService.FileBucket.class)))
        .thenReturn(createTempFile(TEST_ENCRYPTION_KEY));

    when(globalEncryptDecryptClient.decrypt(oldEncryptedDataInDB, oldEncryptedDataInDB.getAccountId(), awsKmsConfig))
        .thenReturn(TEST_ENCRYPTION_KEY.toCharArray());

    encryptedDataAwsToGcpKmsMigrationHandler.handle(oldEncryptedDataInDB);
    verify(globalEncryptDecryptClient, atMost(MAX_RETRY_COUNT))
        .decrypt(oldEncryptedDataInDB, oldEncryptedDataInDB.getAccountId(), awsKmsConfig);
  }

  private File createTempFile(String content) {
    File file = null;
    try {
      file = File.createTempFile(UUIDGenerator.generateUuid().toString(), ".txt");

      // Delete temp file when program exits.
      file.deleteOnExit();

      // Write to temp file
      BufferedWriter out = new BufferedWriter(new FileWriter(file));
      out.write(content);
      out.close();
    } catch (IOException e) {
      log().warn("IOException occured while creating temp file");
    }
    return file;
  }
  private void assertThatOldAndUpdatedRecordAreSame(
      EncryptedData oldEncryptedDataInDB, EncryptedData updatedEncryptedDataInDB) {
    assertThat(updatedEncryptedDataInDB).isNotNull();
    assertThat(updatedEncryptedDataInDB.getEncryptionKey()).isEqualTo(oldEncryptedDataInDB.getEncryptionKey());
    assertThat(updatedEncryptedDataInDB.getEncryptedValue()).isEqualTo(oldEncryptedDataInDB.getEncryptedValue());
    assertThat(updatedEncryptedDataInDB.getEncryptionType()).isEqualTo(oldEncryptedDataInDB.getEncryptionType());
    assertThat(updatedEncryptedDataInDB.getKmsId()).isEqualTo(oldEncryptedDataInDB.getKmsId());
    assertThat(updatedEncryptedDataInDB.getBackupEncryptionType()).isNull();
    assertThat(updatedEncryptedDataInDB.getBackupEncryptionKey()).isNull();
    assertThat(updatedEncryptedDataInDB.getBackupKmsId()).isNull();
    assertThat(updatedEncryptedDataInDB.getEncryptionKey()).isEqualTo(oldEncryptedDataInDB.getEncryptionKey());
    assertThat(updatedEncryptedDataInDB.getLastUpdatedAt()).isEqualTo(oldEncryptedDataInDB.getLastUpdatedAt());
  }

  private EncryptedData getEncryptedData(String encryptionKey, char[] encryptedValue, EncryptionType encryptionType,
      String kmsId, SettingValue.SettingVariableTypes settingVariableTypes) {
    return EncryptedData.builder()
        .encryptionKey(encryptionKey)
        .encryptedValue(encryptedValue)
        .encryptionType(encryptionType)
        .type(settingVariableTypes)
        .kmsId(kmsId)
        .enabled(true)
        .accountId(UUIDGenerator.generateUuid())
        .build();
  }
}