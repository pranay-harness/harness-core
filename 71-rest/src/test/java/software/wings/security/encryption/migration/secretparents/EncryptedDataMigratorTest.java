package software.wings.security.encryption.migration.secretparents;

import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static software.wings.settings.SettingValue.SettingVariableTypes.AZURE;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptionType;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.FeatureName;
import software.wings.beans.SettingAttribute;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedDataParent;
import software.wings.security.encryption.migration.secretparents.migrators.SettingAttributeMigrator;
import software.wings.service.intfc.FeatureFlagService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class EncryptedDataMigratorTest extends WingsBaseTest {
  @Mock private MigratorRegistry migratorRegistry;
  @Mock private SettingAttributeMigrator settingAttributeMigrator;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private FeatureFlagService featureFlagService;
  @Inject @InjectMocks EncryptedDataMigrator encryptedDataMigrator;
  private EncryptedData encryptedData;

  @Before
  public void setup() {
    initMocks(true);
    Account account = getAccount(AccountType.PAID);
    account.setUuid(wingsPersistence.save(account));

    Set<String> parentIds = new HashSet<>();
    parentIds.add(UUIDGenerator.generateUuid());
    parentIds.add(UUIDGenerator.generateUuid());
    parentIds.add(UUIDGenerator.generateUuid());
    parentIds.add(UUIDGenerator.generateUuid());
    parentIds.add(UUIDGenerator.generateUuid());

    encryptedData = EncryptedData.builder()
                        .encryptionKey("plainTextKey")
                        .encryptedValue("encryptedValue".toCharArray())
                        .encryptionType(EncryptionType.KMS)
                        .type(AZURE)
                        .kmsId(UUIDGenerator.generateUuid())
                        .enabled(true)
                        .accountId(account.getUuid())
                        .parentIds(parentIds)
                        .build();

    encryptedData = wingsPersistence.get(EncryptedData.class, wingsPersistence.save(encryptedData));
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testShouldMigrate_shouldReturnTrue() {
    boolean returnValue = encryptedDataMigrator.shouldMigrate(encryptedData);
    assertThat(returnValue).isTrue();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testShouldMigrate_shouldReturnFalse_Condition1() {
    featureFlagService.enableAccount(FeatureName.SECRET_PARENTS_MIGRATED, encryptedData.getAccountId());
    boolean returnValue = encryptedDataMigrator.shouldMigrate(encryptedData);
    assertThat(returnValue).isFalse();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testShouldMigrate_shouldReturnFalse_Condition2() {
    Set<EncryptedDataParent> encryptedDataParents = encryptedData.getParents();
    encryptedData.setParents(encryptedDataParents);
    boolean returnValue = encryptedDataMigrator.shouldMigrate(encryptedData);
    assertThat(returnValue).isFalse();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testCanMigrate_shouldReturnTrue() {
    when(migratorRegistry.getMigrator(AZURE)).thenReturn(Optional.of((SecretsMigrator) settingAttributeMigrator));
    boolean returnValue = encryptedDataMigrator.canMigrate(encryptedData);
    assertThat(returnValue).isTrue();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testCanMigrate_shouldReturnFalse() {
    when(migratorRegistry.getMigrator(AZURE)).thenReturn(Optional.empty());
    boolean returnValue = encryptedDataMigrator.canMigrate(encryptedData);
    assertThat(returnValue).isFalse();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testMigrateEncryptedDataParents_shouldPass() {
    when(migratorRegistry.getMigrator(AZURE)).thenReturn(Optional.of((SecretsMigrator) settingAttributeMigrator));

    SettingAttribute settingAttribute = mock(SettingAttribute.class);
    List<SettingAttribute> settingAttributes = new ArrayList<>();
    settingAttributes.add(settingAttribute);
    settingAttributes.add(settingAttribute);
    settingAttributes.add(settingAttribute);
    settingAttributes.add(settingAttribute);

    Set<EncryptedDataParent> expectedDataParents = new HashSet<>();
    expectedDataParents.add(new EncryptedDataParent(UUIDGenerator.generateUuid(), AZURE, "test1"));
    expectedDataParents.add(new EncryptedDataParent(UUIDGenerator.generateUuid(), AZURE, "test2"));
    expectedDataParents.add(new EncryptedDataParent(UUIDGenerator.generateUuid(), AZURE, "test3"));

    Set<String> expectedParentIds =
        expectedDataParents.stream().map(EncryptedDataParent::getId).collect(Collectors.toSet());

    Iterator<EncryptedDataParent> iterator = expectedDataParents.iterator();

    when(settingAttributeMigrator.getParents(encryptedData.getParentIds())).thenReturn(settingAttributes);
    when(settingAttributeMigrator.buildEncryptedDataParent(encryptedData.getUuid(), settingAttribute))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(iterator.next()))
        .thenReturn(Optional.of(iterator.next()))
        .thenReturn(Optional.of(iterator.next()));

    Optional<EncryptedData> encryptedDataOptional = encryptedDataMigrator.migrateEncryptedDataParents(encryptedData);

    assertThat(encryptedDataOptional.isPresent()).isTrue();
    encryptedData = encryptedDataOptional.get();
    assertThat(encryptedData.getParentIds()).isEqualTo(expectedParentIds);

    featureFlagService.enableAccount(FeatureName.SECRET_PARENTS_MIGRATED, encryptedData.getAccountId());
    assertThat(encryptedData.getParents()).isEqualTo(expectedDataParents);
  }
}
