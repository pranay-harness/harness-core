package software.wings.security.encryption;

import static io.harness.rule.OwnerRule.UTKARSH;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.FeatureName;
import software.wings.beans.KmsConfig;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@RunWith(Parameterized.class)
public class EncryptedDataTest extends WingsBaseTest {
  @Inject private FeatureFlagService featureFlagService;
  @Inject private WingsPersistence wingsPersistence;
  @Parameter public boolean secretMigrationComplete;
  private EncryptedData encryptedData;
  private SettingVariableTypes encryptedDataType;

  @Parameters
  public static Collection<Object[]> data() {
    return asList(new Object[][] {{true}, {false}});
  }

  @Before
  public void setup() throws Exception {
    Account account = getAccount(AccountType.PAID);
    account.setUuid(wingsPersistence.save(account));

    if (secretMigrationComplete) {
      featureFlagService.enableAccount(FeatureName.SECRET_PARENTS_MIGRATED, account.getUuid());
    }

    KmsConfig kmsConfig = KmsConfig.builder()
                              .kmsArn("kmsArn")
                              .accessKey("accessKey")
                              .name("name")
                              .region("region")
                              .secretKey("secretKey")
                              .build();

    encryptedDataType = SettingVariableTypes.AWS;
    encryptedData = encrypt(account.getUuid(), "testValue".toCharArray(), kmsConfig);
    encryptedData.setType(encryptedDataType);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testAddParent() {
    String id = "id";
    SettingVariableTypes type = SettingVariableTypes.GCP_KMS;
    String fieldName = "fieldName";
    EncryptedDataParent encryptedDataParent = new EncryptedDataParent(id, type, fieldName);
    encryptedData.addParent(encryptedDataParent);
    Set<EncryptedDataParent> parents = encryptedData.getParents();
    assertThat(parents).hasSize(1);
    EncryptedDataParent returnedParent = parents.iterator().next();
    assertThat(returnedParent.getId()).isEqualTo(id);

    if (secretMigrationComplete) {
      assertThat(returnedParent.getFieldName()).isEqualTo(fieldName);
      assertThat(returnedParent.getType()).isEqualTo(type);
      assertThat(encryptedData.areParentIdsEquivalentToParent()).isFalse();
    } else {
      assertThat(returnedParent.getType()).isEqualTo(encryptedDataType);
      assertThat(returnedParent.getFieldName()).isNull();
      assertThat(encryptedData.areParentIdsEquivalentToParent()).isTrue();
    }
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void removeParent() {
    List<EncryptedDataParent> addedParents = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      String id = UUIDGenerator.generateUuid();
      SettingVariableTypes type = SettingVariableTypes.AWS;
      String fieldName = UUIDGenerator.generateUuid();
      EncryptedDataParent encryptedDataParent = new EncryptedDataParent(id, type, fieldName);
      encryptedData.addParent(encryptedDataParent);
      addedParents.add(encryptedDataParent);
    }

    assertThat(encryptedData.getParents()).hasSize(2);
    assertThat(encryptedData.containsParent(addedParents.get(0).getId(), addedParents.get(0).getType())).isTrue();
    assertThat(encryptedData.containsParent(addedParents.get(1).getId(), addedParents.get(1).getType())).isTrue();

    encryptedData.removeParent(addedParents.get(1));
    assertThat(encryptedData.getParents()).hasSize(1);
    assertThat(encryptedData.containsParent(addedParents.get(0).getId(), addedParents.get(0).getType())).isTrue();
    assertThat(encryptedData.containsParent(addedParents.get(1).getId(), addedParents.get(1).getType())).isFalse();
  }
}
