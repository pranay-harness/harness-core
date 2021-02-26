package io.harness.secrets;

import static io.harness.rule.OwnerRule.PIYUSH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import io.harness.SMCoreTestBase;
import io.harness.SecretTestUtils;
import io.harness.beans.EncryptedData;
import io.harness.beans.SecretScopeMetadata;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SecretServiceImplTest extends SMCoreTestBase {
  @Inject SecretService secretService;
  @Inject SecretsRBACService secretsRBACService;
  @Inject SecretsDao secretsDao;

  @Test
  @Owner(developers = PIYUSH)
  @Category(UnitTests.class)
  public void test_itShouldReturnOnlySecretsWithReadPermissionTest() {
    List<String> secretIds = saveDummySerectsInDB();
    List<SecretScopeMetadata> mockSecretScopeMetaData = new ArrayList<>();
    mockSecretScopeMetaData.add(SecretScopeMetadata.builder().secretId(secretIds.get(1)).build());
    when(secretsRBACService.filterSecretsByReadPermission(anyString(), any(List.class), anyString(), anyString()))
        .thenReturn(mockSecretScopeMetaData);
    List<String> secretIdsResponse =
        secretService.filterSecretIdsByReadPermission(new HashSet<>(secretIds), "accountId", "dummy", "dummmy");
    assertThat(secretIdsResponse.size()).isEqualTo(1);
  }

  private List<String> saveDummySerectsInDB() {
    EncryptedData secretThatCannotBeRead = SecretTestUtils.getInlineSecretText();
    secretThatCannotBeRead.setAccountId("someOtherAccountId");
    EncryptedData secretThatCanbeRead = SecretTestUtils.getInlineSecretText();
    secretsDao.saveSecret(secretThatCannotBeRead);
    secretsDao.saveSecret(secretThatCanbeRead);
    return Arrays.asList(secretThatCanbeRead.getUuid(), secretThatCannotBeRead.getUuid());
  }
}
