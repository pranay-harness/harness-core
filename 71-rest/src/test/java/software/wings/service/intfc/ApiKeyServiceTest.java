package software.wings.service.intfc;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACCOUNT_KEY;
import static software.wings.utils.WingsTestConstants.USER_GROUP_ID;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.category.element.UnitTests;
import io.harness.exception.UnauthorizedException;
import io.harness.exception.WingsException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.ApiKeyEntry;
import software.wings.beans.security.UserGroup;
import software.wings.security.encryption.SimpleEncryption;
import software.wings.utils.Validator;

public class ApiKeyServiceTest extends WingsBaseTest {
  @Mock private AccountService accountService;
  @Mock private UserGroupService userGroupService;
  @Inject @InjectMocks private ApiKeyService apiKeyService;

  @Test
  @Category(UnitTests.class)
  public void testGenerate() {
    ApiKeyEntry savedApiKeyEntry = generateKey();
    assertThat(savedApiKeyEntry).isNotNull();
    assertThat(savedApiKeyEntry.getDecryptedKey()).isNotEmpty();
  }

  private ApiKeyEntry generateKey() {
    Account account = anAccount().withUuid(ACCOUNT_ID).withAccountKey(ACCOUNT_KEY).build();
    doReturn(account).when(accountService).get(ACCOUNT_ID);
    UserGroup userGroup = UserGroup.builder().uuid(USER_GROUP_ID).build();
    PageResponse pageResponse = aPageResponse().withResponse(asList(userGroup)).build();
    doReturn(pageResponse).when(userGroupService).list(anyString(), any(PageRequest.class), anyBoolean());
    ApiKeyEntry apiKeyEntry =
        ApiKeyEntry.builder().name("name1").accountId(ACCOUNT_ID).userGroupIds(asList(USER_GROUP_ID)).build();
    return apiKeyService.generate(ACCOUNT_ID, apiKeyEntry);
  }

  @Test
  @Category(UnitTests.class)
  public void testUpdate() {
    ApiKeyEntry apiKeyEntry = generateKey();
    ApiKeyEntry apiKeyEntryForUpdate = ApiKeyEntry.builder().name("newName").build();
    ApiKeyEntry updatedApiKeyEntry =
        apiKeyService.update(apiKeyEntry.getUuid(), apiKeyEntry.getAccountId(), apiKeyEntryForUpdate);
    assertThat(updatedApiKeyEntry).isNotNull();
    assertThat(updatedApiKeyEntry.getUuid()).isEqualTo(apiKeyEntry.getUuid());
    assertThat(updatedApiKeyEntry.getDecryptedKey()).isEqualTo(apiKeyEntry.getDecryptedKey());

    apiKeyEntryForUpdate = ApiKeyEntry.builder().userGroupIds(asList(USER_GROUP_ID)).build();
    updatedApiKeyEntry = apiKeyService.update(apiKeyEntry.getUuid(), apiKeyEntry.getAccountId(), apiKeyEntryForUpdate);
    assertThat(updatedApiKeyEntry).isNotNull();
    assertThat(updatedApiKeyEntry.getUuid()).isEqualTo(apiKeyEntry.getUuid());
    assertThat(updatedApiKeyEntry.getUserGroupIds()).isEqualTo(apiKeyEntry.getUserGroupIds());
  }

  @Test(expected = WingsException.class)
  @Category(UnitTests.class)
  public void testDelete() {
    ApiKeyEntry apiKeyEntry = generateKey();
    apiKeyService.delete(ACCOUNT_ID, apiKeyEntry.getUuid());
    apiKeyService.get(apiKeyEntry.getUuid(), apiKeyEntry.getAccountId());
  }

  private SimpleEncryption getSimpleEncryption(String accountId) {
    Account account = accountService.get(accountId);
    Validator.notNullCheck("Account", account);
    return new SimpleEncryption(account.getAccountKey().toCharArray());
  }

  @Test
  @Category(UnitTests.class)
  public void testGet() {
    ApiKeyEntry apiKeyEntry = generateKey();
    ApiKeyEntry apiKeyEntryFromGet = apiKeyService.get(apiKeyEntry.getUuid(), ACCOUNT_ID);
    assertNotNull(apiKeyEntryFromGet);
    String key = apiKeyEntryFromGet.getDecryptedKey();
    assertThat(key).isEqualTo(apiKeyEntry.getDecryptedKey());
  }

  @Test
  @Category(UnitTests.class)
  public void testList() {
    generateKey();
    PageRequest request = PageRequestBuilder.aPageRequest().addFilter("accountId", Operator.EQ, ACCOUNT_ID).build();
    PageResponse pageResponse = apiKeyService.list(request, ACCOUNT_ID);
    assertThat(pageResponse.getResponse()).isNotEmpty();
  }

  @Test
  @Category(UnitTests.class)
  public void testValidate() {
    ApiKeyEntry apiKeyEntry = generateKey();
    try {
      apiKeyService.validate(apiKeyEntry.getDecryptedKey(), ACCOUNT_ID);
    } catch (UnauthorizedException ex) {
      fail("Validation failed: " + ex.getMessage());
    }
  }
}
