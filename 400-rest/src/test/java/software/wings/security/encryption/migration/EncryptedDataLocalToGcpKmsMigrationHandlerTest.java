package software.wings.security.encryption.migration;

import static io.harness.beans.FeatureName.ACTIVE_MIGRATION_FROM_LOCAL_TO_GCP_KMS;
import static io.harness.mongo.iterator.MongoPersistenceIterator.MongoPersistenceIteratorBuilder;
import static io.harness.rule.OwnerRule.KARAN;
import static io.harness.security.encryption.EncryptionType.GCP_KMS;
import static io.harness.security.encryption.EncryptionType.LOCAL;

import static software.wings.security.encryption.migration.EncryptedDataLocalToGcpKmsMigrationHandler.MAX_RETRY_COUNT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.beans.EncryptedData;
import io.harness.beans.EncryptedData.EncryptedDataKeys;
import io.harness.beans.FeatureFlag;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.ff.FeatureFlagService;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceProvider;
import io.harness.rule.Owner;
import io.harness.secrets.SecretService;
import io.harness.security.encryption.EncryptionType;

import software.wings.WingsBaseTest;
import software.wings.beans.GcpKmsConfig;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.security.GcpSecretsManagerService;
import software.wings.service.intfc.security.LocalSecretManagerService;
import software.wings.settings.SettingVariableTypes;

import com.google.inject.Inject;
import groovy.util.logging.Slf4j;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({EncryptedDataLocalToGcpKmsMigrationHandler.class, PersistenceIteratorFactory.class})
@PowerMockIgnore({"javax.security.*", "javax.crypto.*", "javax.net.*"})
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class EncryptedDataLocalToGcpKmsMigrationHandlerTest extends WingsBaseTest {
  @Inject WingsPersistence wingsPersistence;
  @Mock FeatureFlagService featureFlagService;
  @Mock PersistenceIteratorFactory persistenceIteratorFactory;
  @Mock GcpSecretsManagerService gcpSecretsManagerService;
  @Mock LocalSecretManagerService localSecretManagerService;
  @Mock MorphiaPersistenceProvider<EncryptedData> persistenceProvider;
  @Mock SecretService secretService;
  @Mock GcpKmsConfig gcpKmsConfig;
  ArgumentCaptor<MongoPersistenceIteratorBuilder> captor =
      ArgumentCaptor.forClass(MongoPersistenceIteratorBuilder.class);

  EncryptedDataLocalToGcpKmsMigrationHandler encryptedDataLocalToGcpKmsMigrationHandler;
  EncryptedDataLocalToGcpKmsMigrationHandler spyEncryptedDataLocalToGcpKmsMigrationHandler;

  static final String TEST_ACCOUNT_1 = "testAccount1";
  static final String LOCAL_SECRET_MANAGER_UUID = TEST_ACCOUNT_1;
  static final String GCP_KMS_UUID = UUIDGenerator.generateUuid();
  static final String TEST_ENCRYPTION_KEY = "testKey";

  @Before
  public void setup() {
    encryptedDataLocalToGcpKmsMigrationHandler =
        new EncryptedDataLocalToGcpKmsMigrationHandler(wingsPersistence, featureFlagService, persistenceIteratorFactory,
            gcpSecretsManagerService, localSecretManagerService, persistenceProvider, secretService);
    spyEncryptedDataLocalToGcpKmsMigrationHandler = spy(
        new EncryptedDataLocalToGcpKmsMigrationHandler(wingsPersistence, featureFlagService, persistenceIteratorFactory,
            gcpSecretsManagerService, localSecretManagerService, persistenceProvider, secretService));
    when(persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(any(), any(), any()))
        .thenAnswer(
            invocationOnMock -> invocationOnMock.getArgumentAt(2, MongoPersistenceIteratorBuilder.class).build());
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testRegisterIteratorsWhenGcpKmsConfigIsNull() {
    when(gcpSecretsManagerService.getGlobalKmsConfig()).thenReturn(null);
    encryptedDataLocalToGcpKmsMigrationHandler.registerIterators();
    verify(gcpSecretsManagerService, times(1)).getGlobalKmsConfig();
    verify(featureFlagService, times(0)).getFeatureFlag(ACTIVE_MIGRATION_FROM_LOCAL_TO_GCP_KMS);
    verify(persistenceIteratorFactory, times(0))
        .createPumpIteratorWithDedicatedThreadPool(any(), eq(EncryptedData.class), captor.capture());
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testRegisterIteratorsWhenFlagNotPresentInDB() {
    when(gcpSecretsManagerService.getGlobalKmsConfig()).thenReturn(gcpKmsConfig);
    when(featureFlagService.getFeatureFlag(ACTIVE_MIGRATION_FROM_LOCAL_TO_GCP_KMS)).thenReturn(Optional.empty());
    encryptedDataLocalToGcpKmsMigrationHandler.registerIterators();
    verify(gcpSecretsManagerService, times(1)).getGlobalKmsConfig();
    verify(featureFlagService, times(1)).getFeatureFlag(ACTIVE_MIGRATION_FROM_LOCAL_TO_GCP_KMS);
    verify(persistenceIteratorFactory, times(0))
        .createPumpIteratorWithDedicatedThreadPool(any(), eq(EncryptedData.class), captor.capture());
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testRegisterIteratorsWhenFlagNotEnabled() {
    when(gcpSecretsManagerService.getGlobalKmsConfig()).thenReturn(gcpKmsConfig);
    when(featureFlagService.getFeatureFlag(ACTIVE_MIGRATION_FROM_LOCAL_TO_GCP_KMS))
        .thenReturn(Optional.of(FeatureFlag.builder().build()));
    encryptedDataLocalToGcpKmsMigrationHandler.registerIterators();
    verify(gcpSecretsManagerService, times(1)).getGlobalKmsConfig();
    verify(featureFlagService, times(1)).getFeatureFlag(ACTIVE_MIGRATION_FROM_LOCAL_TO_GCP_KMS);
    verify(persistenceIteratorFactory, times(0))
        .createPumpIteratorWithDedicatedThreadPool(any(), eq(EncryptedData.class), captor.capture());
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testRegisterIteratorsWhenFlagEnabledGlobally() throws NoSuchFieldException, IllegalAccessException {
    when(gcpSecretsManagerService.getGlobalKmsConfig()).thenReturn(gcpKmsConfig);
    when(featureFlagService.getFeatureFlag(ACTIVE_MIGRATION_FROM_LOCAL_TO_GCP_KMS))
        .thenReturn(Optional.of(FeatureFlag.builder().enabled(true).build()));
    encryptedDataLocalToGcpKmsMigrationHandler.registerIterators();
    verify(gcpSecretsManagerService, times(1)).getGlobalKmsConfig();
    verify(featureFlagService, times(1)).getFeatureFlag(ACTIVE_MIGRATION_FROM_LOCAL_TO_GCP_KMS);
    verify(persistenceIteratorFactory, times(1))
        .createPumpIteratorWithDedicatedThreadPool(any(), eq(EncryptedData.class), captor.capture());

    MongoPersistenceIterator<EncryptedData, MorphiaFilterExpander<EncryptedData>> persistenceIterator =
        (MongoPersistenceIterator<EncryptedData, MorphiaFilterExpander<EncryptedData>>) captor.getValue().build();
    assertThat(persistenceIterator).isNotNull();

    Field f = persistenceIterator.getClass().getDeclaredField("fieldName");
    f.setAccessible(true);
    String fieldName = (String) f.get(persistenceIterator);
    assertThat(fieldName).isEqualTo(EncryptedDataKeys.nextLocalToGcpKmsMigrationIteration);

    f = persistenceIterator.getClass().getDeclaredField("filterExpander");
    f.setAccessible(true);
    MorphiaFilterExpander<EncryptedData> filterExpander =
        (MorphiaFilterExpander<EncryptedData>) f.get(persistenceIterator);
    assertThat(filterExpander).isNotNull();
    final Query<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class).order(Sort.ascending(fieldName));
    filterExpander.filter(query);
    assertThat(query.toString()).contains("{\"accountId\": {\"$exists\": true}");
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testRegisterIteratorsWhenFlagEnabledForFewAccounts() throws NoSuchFieldException, IllegalAccessException {
    when(gcpSecretsManagerService.getGlobalKmsConfig()).thenReturn(gcpKmsConfig);
    String testAccount2 = "testAccount2";
    Set<String> accountIds = Stream.of(TEST_ACCOUNT_1, testAccount2).collect(Collectors.toCollection(HashSet::new));
    when(featureFlagService.getFeatureFlag(ACTIVE_MIGRATION_FROM_LOCAL_TO_GCP_KMS))
        .thenReturn(Optional.of(FeatureFlag.builder().accountIds(accountIds).build()));
    encryptedDataLocalToGcpKmsMigrationHandler.registerIterators();
    verify(featureFlagService, times(1)).getFeatureFlag(ACTIVE_MIGRATION_FROM_LOCAL_TO_GCP_KMS);
    verify(gcpSecretsManagerService, times(1)).getGlobalKmsConfig();
    verify(persistenceIteratorFactory, times(1))
        .createPumpIteratorWithDedicatedThreadPool(any(), eq(EncryptedData.class), captor.capture());
    MongoPersistenceIterator<EncryptedData, MorphiaFilterExpander<EncryptedData>> persistenceIterator =
        (MongoPersistenceIterator<EncryptedData, MorphiaFilterExpander<EncryptedData>>) captor.getValue().build();
    assertThat(persistenceIterator).isNotNull();

    Field f = persistenceIterator.getClass().getDeclaredField("fieldName");
    f.setAccessible(true);
    String fieldName = (String) f.get(persistenceIterator);
    assertThat(fieldName).isEqualTo(EncryptedDataKeys.nextLocalToGcpKmsMigrationIteration);

    f = persistenceIterator.getClass().getDeclaredField("filterExpander");
    f.setAccessible(true);
    MorphiaFilterExpander<EncryptedData> filterExpander =
        (MorphiaFilterExpander<EncryptedData>) f.get(persistenceIterator);
    assertThat(filterExpander).isNotNull();
    final Query<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class).order(Sort.ascending(fieldName));
    filterExpander.filter(query);
    String queryString = query.toString();
    assertThat(queryString).contains("{\"accountId\": {\"$in\": [");
    assertThat(queryString).contains(TEST_ACCOUNT_1);
    assertThat(queryString).contains(testAccount2);
  }

  private EncryptedData getEncryptedData(String encryptionKey, char[] encryptedValue, EncryptionType encryptionType,
      String kmsId, SettingVariableTypes settingVariableTypes, String accountId) {
    return EncryptedData.builder()
        .encryptionKey(encryptionKey)
        .encryptedValue(encryptedValue)
        .encryptionType(encryptionType)
        .type(settingVariableTypes)
        .kmsId(kmsId)
        .enabled(true)
        .accountId(accountId)
        .build();
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testHandleWhenFFisDisabled() {
    EncryptedData encryptedData = getEncryptedData(TEST_ENCRYPTION_KEY, TEST_ENCRYPTION_KEY.toCharArray(), LOCAL,
        LOCAL_SECRET_MANAGER_UUID, SettingVariableTypes.SECRET_TEXT, TEST_ACCOUNT_1);
    String encryptedDataId = wingsPersistence.save(encryptedData);
    EncryptedData oldEncryptedDataInDB = wingsPersistence.get(EncryptedData.class, encryptedDataId);

    when(featureFlagService.isEnabled(ACTIVE_MIGRATION_FROM_LOCAL_TO_GCP_KMS, oldEncryptedDataInDB.getAccountId()))
        .thenReturn(false);
    encryptedDataLocalToGcpKmsMigrationHandler.handle(oldEncryptedDataInDB);
    EncryptedData updatedEncryptedDataInDB = wingsPersistence.get(EncryptedData.class, encryptedDataId);
    assertThatOldAndUpdatedRecordAreSame(oldEncryptedDataInDB, updatedEncryptedDataInDB);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testHandleForEncryptedValueAsNull() throws NoSuchFieldException, IllegalAccessException {
    EncryptedData encryptedData = getEncryptedData(
        TEST_ENCRYPTION_KEY, null, LOCAL, LOCAL_SECRET_MANAGER_UUID, SettingVariableTypes.SECRET_TEXT, TEST_ACCOUNT_1);
    String encryptedDataId = wingsPersistence.save(encryptedData);

    EncryptedData oldEncryptedDataInDB = wingsPersistence.get(EncryptedData.class, encryptedDataId);

    when(featureFlagService.isEnabled(ACTIVE_MIGRATION_FROM_LOCAL_TO_GCP_KMS, oldEncryptedDataInDB.getAccountId()))
        .thenReturn(true);
    Field f = encryptedDataLocalToGcpKmsMigrationHandler.getClass().getDeclaredField("gcpKmsConfig");
    f.setAccessible(true);
    f.set(encryptedDataLocalToGcpKmsMigrationHandler, gcpKmsConfig);
    when(((GcpKmsConfig) f.get(encryptedDataLocalToGcpKmsMigrationHandler)).getUuid()).thenReturn(GCP_KMS_UUID);
    encryptedDataLocalToGcpKmsMigrationHandler.handle(oldEncryptedDataInDB);

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
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testHandleWhenMigrationFailed() {
    EncryptedData encryptedData = getEncryptedData(TEST_ENCRYPTION_KEY, TEST_ENCRYPTION_KEY.toCharArray(), LOCAL,
        LOCAL_SECRET_MANAGER_UUID, SettingVariableTypes.SECRET_TEXT, TEST_ACCOUNT_1);
    String encryptedDataId = wingsPersistence.save(encryptedData);
    EncryptedData oldEncryptedDataInDB = wingsPersistence.get(EncryptedData.class, encryptedDataId);
    when(featureFlagService.isEnabled(ACTIVE_MIGRATION_FROM_LOCAL_TO_GCP_KMS, oldEncryptedDataInDB.getAccountId()))
        .thenReturn(true);
    doReturn(false).when(spyEncryptedDataLocalToGcpKmsMigrationHandler).migrateToGcpKMS(oldEncryptedDataInDB);

    spyEncryptedDataLocalToGcpKmsMigrationHandler.handle(oldEncryptedDataInDB);
    EncryptedData updatedEncryptedDataInDB = wingsPersistence.get(EncryptedData.class, encryptedDataId);
    assertThatOldAndUpdatedRecordAreSame(oldEncryptedDataInDB, updatedEncryptedDataInDB);
    verify(spyEncryptedDataLocalToGcpKmsMigrationHandler, times(MAX_RETRY_COUNT)).migrateToGcpKMS(oldEncryptedDataInDB);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testHandleWhenMigrationSuccessful() {
    EncryptedData encryptedData = getEncryptedData(TEST_ENCRYPTION_KEY, TEST_ENCRYPTION_KEY.toCharArray(), LOCAL,
        LOCAL_SECRET_MANAGER_UUID, SettingVariableTypes.SECRET_TEXT, TEST_ACCOUNT_1);
    String encryptedDataId = wingsPersistence.save(encryptedData);
    EncryptedData oldEncryptedDataInDB = wingsPersistence.get(EncryptedData.class, encryptedDataId);

    when(featureFlagService.isEnabled(ACTIVE_MIGRATION_FROM_LOCAL_TO_GCP_KMS, oldEncryptedDataInDB.getAccountId()))
        .thenReturn(true);
    doReturn(true).when(spyEncryptedDataLocalToGcpKmsMigrationHandler).migrateToGcpKMS(oldEncryptedDataInDB);

    spyEncryptedDataLocalToGcpKmsMigrationHandler.handle(oldEncryptedDataInDB);
    verify(spyEncryptedDataLocalToGcpKmsMigrationHandler, atMost(MAX_RETRY_COUNT))
        .migrateToGcpKMS(oldEncryptedDataInDB);
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
}
