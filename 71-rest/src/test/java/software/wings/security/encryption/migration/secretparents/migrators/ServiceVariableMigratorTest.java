package software.wings.security.encryption.migration.secretparents.migrators;

import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;
import static software.wings.beans.Environment.GLOBAL_ENV_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.encryption.EncryptionReflectUtils;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptionType;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.EntityType;
import software.wings.beans.ServiceVariable;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedDataParent;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ServiceVariableMigratorTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ServiceVariableMigrator serviceVariableMigrator;
  private List<ServiceVariable> serviceVariableList;

  @Before
  public void setup() {
    initMocks(this);
    Account account = getAccount(AccountType.PAID);
    account.setUuid(wingsPersistence.save(account));
    serviceVariableList = new ArrayList<>();

    EncryptedData encryptedData = EncryptedData.builder()
                                      .encryptionKey("plainTextKey")
                                      .encryptedValue("encryptedValue".toCharArray())
                                      .encryptionType(EncryptionType.LOCAL)
                                      .type(SettingVariableTypes.SECRET_TEXT)
                                      .kmsId(UUIDGenerator.generateUuid())
                                      .enabled(true)
                                      .accountId(account.getUuid())
                                      .name("xyz")
                                      .build();

    ServiceVariable serviceVariable = ServiceVariable.builder()
                                          .templateId(UUIDGenerator.generateUuid())
                                          .envId(GLOBAL_ENV_ID)
                                          .entityType(EntityType.SERVICE)
                                          .entityId(UUIDGenerator.generateUuid())
                                          .parentServiceVariableId(UUIDGenerator.generateUuid())
                                          .overrideType(ServiceVariable.OverrideType.ALL)
                                          .accountId(account.getUuid())
                                          .name("service_variable_1")
                                          .encryptedValue(wingsPersistence.save(encryptedData))
                                          .type(ServiceVariable.Type.ENCRYPTED_TEXT)
                                          .build();

    serviceVariableList.add(wingsPersistence.get(ServiceVariable.class, wingsPersistence.save(serviceVariable)));

    encryptedData.setUuid(null);
    encryptedData.setName("abc");
    serviceVariable.setName("service_variable_2");
    serviceVariable.setEncryptedValue(wingsPersistence.save(encryptedData));
    serviceVariable.setUuid(null);
    serviceVariableList.add(wingsPersistence.get(ServiceVariable.class, wingsPersistence.save(serviceVariable)));

    serviceVariable.setName("service_variable_3");
    serviceVariable.setEncryptedValue(null);
    serviceVariable.setValue("plaintext".toCharArray());
    serviceVariable.setType(ServiceVariable.Type.TEXT);
    serviceVariable.setUuid(null);
    serviceVariableList.add(wingsPersistence.get(ServiceVariable.class, wingsPersistence.save(serviceVariable)));
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testGetParents_shouldPass() {
    Set<String> parentIds = new HashSet<>();
    parentIds.add(serviceVariableList.get(0).getUuid());
    parentIds.add(serviceVariableList.get(2).getUuid());
    parentIds.add(UUIDGenerator.generateUuid());
    List<ServiceVariable> serviceVariables = serviceVariableMigrator.getParents(parentIds);
    assertThat(serviceVariables).hasSize(1);
    assertThat(serviceVariables.get(0)).isEqualTo(serviceVariableList.get(0));
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testBuildEncryptedDataParents_shouldPass() {
    String encryptedFieldName =
        EncryptionReflectUtils.getEncryptedFieldTag(serviceVariableList.get(0).getEncryptedFields().get(0));
    List<EncryptedDataParent> expectedDataParents = new ArrayList<>();
    expectedDataParents.add(new EncryptedDataParent(
        serviceVariableList.get(0).getUuid(), serviceVariableList.get(0).getSettingType(), encryptedFieldName));
    expectedDataParents.add(new EncryptedDataParent(
        serviceVariableList.get(1).getUuid(), serviceVariableList.get(1).getSettingType(), encryptedFieldName));

    Optional<EncryptedDataParent> parent = serviceVariableMigrator.buildEncryptedDataParent(
        serviceVariableList.get(0).getEncryptedValue(), serviceVariableList.get(0));
    assertThat(parent.isPresent()).isTrue();
    assertThat(parent.get()).isEqualTo(expectedDataParents.get(0));

    parent = serviceVariableMigrator.buildEncryptedDataParent(
        serviceVariableList.get(0).getEncryptedValue(), serviceVariableList.get(1));
    assertThat(parent.isPresent()).isFalse();

    parent = serviceVariableMigrator.buildEncryptedDataParent(
        serviceVariableList.get(1).getEncryptedValue(), serviceVariableList.get(0));
    assertThat(parent.isPresent()).isFalse();

    parent = serviceVariableMigrator.buildEncryptedDataParent(
        serviceVariableList.get(1).getEncryptedValue(), serviceVariableList.get(1));
    assertThat(parent.isPresent()).isTrue();
    assertThat(parent.get()).isEqualTo(expectedDataParents.get(1));
  }
}
