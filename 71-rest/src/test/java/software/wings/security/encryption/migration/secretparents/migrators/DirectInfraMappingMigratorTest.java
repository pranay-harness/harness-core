package software.wings.security.encryption.migration.secretparents.migrators;

import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedDataParent;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class DirectInfraMappingMigratorTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;
  @Inject DirectInfraMappingMigrator directInfraMappingMigrator;
  private InfrastructureMapping infrastructureMapping;

  @Before
  public void setup() {
    initMocks(this);
    Account account = getAccount(AccountType.PAID);
    account.setUuid(wingsPersistence.save(account));

    infrastructureMapping = DirectKubernetesInfrastructureMapping.builder()
                                .infraMappingType(InfrastructureMappingType.DIRECT_KUBERNETES.name())
                                .accountId(account.getUuid())
                                .cloudProviderId(UUIDGenerator.generateUuid())
                                .build();

    infrastructureMapping =
        wingsPersistence.get(InfrastructureMapping.class, wingsPersistence.save(infrastructureMapping));
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testGetParents_shouldPass() {
    Set<String> parentIds = new HashSet<>();
    parentIds.add(infrastructureMapping.getUuid());
    parentIds.add(UUIDGenerator.generateUuid());

    List<InfrastructureMapping> returnedInfraMappings = directInfraMappingMigrator.getParents(parentIds);
    assertThat(returnedInfraMappings).hasSize(1);
    assertThat(returnedInfraMappings.get(0)).isEqualTo(infrastructureMapping);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testBuildEncryptedDataParents_shouldPass() {
    SettingVariableTypes type = SettingVariableTypes.INFRASTRUCTURE_MAPPING;
    String encryptedFieldName = type.toString();
    EncryptedDataParent expectedParent =
        new EncryptedDataParent(infrastructureMapping.getUuid(), type, encryptedFieldName);

    Optional<EncryptedDataParent> parent =
        directInfraMappingMigrator.buildEncryptedDataParent(UUIDGenerator.generateUuid(), infrastructureMapping);
    assertThat(parent.isPresent()).isTrue();
    assertThat(parent.get()).isEqualTo(expectedParent);
  }
}
