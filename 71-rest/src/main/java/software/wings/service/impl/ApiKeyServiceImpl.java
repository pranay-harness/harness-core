package software.wings.service.impl;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.beans.SearchFilter.Operator.IN;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.WingsException.USER;
import static io.harness.mongo.MongoUtils.setUnset;
import static java.lang.System.currentTimeMillis;
import static org.mindrot.jbcrypt.BCrypt.checkpw;
import static org.mindrot.jbcrypt.BCrypt.hashpw;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import com.google.common.base.Charsets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.exception.UnauthorizedException;
import org.mindrot.jbcrypt.BCrypt;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Account;
import software.wings.beans.ApiKeyEntry;
import software.wings.beans.ApiKeyEntry.ApiKeyEntryKeys;
import software.wings.beans.security.UserGroup;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.SimpleEncryption;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.ApiKeyService;
import software.wings.service.intfc.UserGroupService;
import software.wings.utils.CryptoUtils;
import software.wings.utils.Validator;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
public class ApiKeyServiceImpl implements ApiKeyService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AccountService accountService;
  @Inject private UserGroupService userGroupService;
  @Inject private ApiKeyServiceCommunityEdition apiKeyServiceCommunity;

  private static String DELIMITER = "::";

  private SimpleEncryption getSimpleEncryption(String accountId) {
    Account account = accountService.get(accountId);
    Validator.notNullCheck("Account is null for accountId: " + accountId, account);
    return new SimpleEncryption(account.getAccountKey().toCharArray());
  }

  @Override
  public ApiKeyEntry generate(String accountId, ApiKeyEntry apiKeyEntry) {
    if (accountService.isCommunityAccount(accountId)) {
      return apiKeyServiceCommunity.generate(accountId, apiKeyEntry);
    }

    int KEY_LEN = 80;
    String randomKey = accountId + DELIMITER + CryptoUtils.secureRandAlphaNumString(KEY_LEN);
    String apiKey = Base64.getEncoder().encodeToString(randomKey.getBytes(Charsets.UTF_8));
    ApiKeyEntry apiKeyEntryToBeSaved =
        ApiKeyEntry.builder()
            .uuid(generateUuid())
            .userGroupIds(apiKeyEntry.getUserGroupIds())
            .name(apiKeyEntry.getName())
            .createdAt(currentTimeMillis())
            .encryptedKey(getSimpleEncryption(accountId).encryptChars(apiKey.toCharArray()))
            .hashOfKey(hashpw(apiKey, BCrypt.gensalt()))
            .accountId(accountId)
            .build();
    String id = wingsPersistence.save(apiKeyEntryToBeSaved);
    return get(id, accountId);
  }

  @Override
  public ApiKeyEntry update(String uuid, String accountId, ApiKeyEntry apiKeyEntry) {
    if (accountService.isCommunityAccount(accountId)) {
      return apiKeyServiceCommunity.update(uuid, accountId, apiKeyEntry);
    }

    Validator.notNullCheck("ApiKeyEntry is null", apiKeyEntry, USER);
    Validator.notNullCheck("uuid is null for the given api key entry", uuid, USER);
    Validator.notNullCheck(UserGroup.ACCOUNT_ID_KEY, accountId, USER);

    UpdateOperations<ApiKeyEntry> operations = wingsPersistence.createUpdateOperations(ApiKeyEntry.class);
    setUnset(operations, "name", apiKeyEntry.getName());
    setUnset(operations, "userGroupIds", apiKeyEntry.getUserGroupIds());

    Query<ApiKeyEntry> query = wingsPersistence.createQuery(ApiKeyEntry.class)
                                   .filter(ID_KEY, uuid)
                                   .filter(UserGroup.ACCOUNT_ID_KEY, accountId);
    wingsPersistence.update(query, operations);
    return get(uuid, accountId);
  }

  private void loadUserGroupsForApiKeys(List<ApiKeyEntry> apiKeyEntries, String accountId) {
    if (isEmpty(apiKeyEntries)) {
      return;
    }

    PageRequest<UserGroup> req =
        aPageRequest().addFilter("accountId", Operator.EQ, accountId).addFieldsIncluded("_id", "name").build();
    PageResponse<UserGroup> res = userGroupService.list(accountId, req, false);
    List<UserGroup> allUserGroupList = res.getResponse();
    if (isEmpty(allUserGroupList)) {
      return;
    }

    Map<String, UserGroup> idUserGroupMap =
        allUserGroupList.stream().collect(Collectors.toMap(ug -> ug.getUuid(), ug -> ug));

    apiKeyEntries.forEach(apiKeyEntry -> {
      List<String> userGroupIds = apiKeyEntry.getUserGroupIds();
      if (isEmpty(userGroupIds)) {
        return;
      }

      List<UserGroup> userGroupList = new ArrayList<>();

      userGroupIds.forEach(userGroupId -> {
        UserGroup userGroup = idUserGroupMap.get(userGroupId);
        userGroupList.add(userGroup);
      });

      apiKeyEntry.setUserGroups(userGroupList);
    });
  }

  private List<UserGroup> getUserGroupsForApiKey(List<String> userGroupIds, String accountId, boolean details) {
    if (isEmpty(userGroupIds)) {
      return Collections.emptyList();
    }

    PageRequestBuilder pageRequestBuilder =
        aPageRequest().addFilter("accountId", Operator.EQ, accountId).addFilter("_id", IN, userGroupIds.toArray());
    if (!details) {
      pageRequestBuilder.addFieldsIncluded("_id", "name");
    }

    return userGroupService.list(accountId, pageRequestBuilder.build(), false).getResponse();
  }

  @Override
  public PageResponse<ApiKeyEntry> list(PageRequest<ApiKeyEntry> pageRequest, String accountId) {
    PageResponse<ApiKeyEntry> response = wingsPersistence.query(ApiKeyEntry.class, pageRequest);
    loadUserGroupsForApiKeys(response.getResponse(), accountId);
    return response;
  }

  @Override
  public ApiKeyEntry get(String uuid, String accountId) {
    ApiKeyEntry entry = wingsPersistence.createQuery(ApiKeyEntry.class)
                            .filter(ApiKeyEntryKeys.accountId, accountId)
                            .filter(ID_KEY, uuid)
                            .get();
    return buildApiKeyEntry(uuid, entry, false);
  }

  private ApiKeyEntry buildApiKeyEntry(String uuid, ApiKeyEntry entry, boolean details) {
    Validator.notNullCheck("apiKeyEntry is null for id: " + uuid, entry);
    String decryptedKey = new String(getSimpleEncryption(entry.getAccountId()).decryptChars(entry.getEncryptedKey()));

    return ApiKeyEntry.builder()
        .uuid(entry.getUuid())
        .userGroupIds(entry.getUserGroupIds())
        .name(entry.getName())
        .accountId(entry.getAccountId())
        .decryptedKey(decryptedKey)
        .userGroups(
            getUserGroupsForApiKey(entry.getUserGroupIds() != null ? entry.getUserGroupIds() : Collections.emptyList(),
                entry.getAccountId(), details))
        .build();
  }

  @Override
  public String getAccountIdFromApiKey(String apiKey) {
    String decodedApiKey = new String(Base64.getDecoder().decode(apiKey), Charsets.UTF_8);
    String[] split = decodedApiKey.split(DELIMITER);
    return split[0];
  }

  @Override
  public void delete(String accountId, String uuid) {
    wingsPersistence.delete(ApiKeyEntry.class, uuid);
  }

  @Override
  public boolean deleteAll(String accountId) {
    Query<ApiKeyEntry> query =
        wingsPersistence.createQuery(ApiKeyEntry.class).filter(ApiKeyEntryKeys.accountId, accountId);

    return wingsPersistence.delete(query);
  }

  @Override
  public void validate(String key, String accountId) {
    PageRequest<ApiKeyEntry> pageRequest = aPageRequest().addFilter(ApiKeyEntryKeys.accountId, EQ, accountId).build();
    if (!wingsPersistence.query(ApiKeyEntry.class, pageRequest)
             .getResponse()
             .stream()
             .anyMatch(apiKeyEntry -> checkpw(key, apiKeyEntry.getHashOfKey()))) {
      throw new UnauthorizedException("Invalid Api Key", USER);
    }
  }

  @Override
  public ApiKeyEntry getByKey(String key, String accountId, boolean details) {
    PageRequest<ApiKeyEntry> pageRequest = aPageRequest().addFilter(ApiKeyEntryKeys.accountId, EQ, accountId).build();
    Optional<ApiKeyEntry> apiKeyEntryOptional = wingsPersistence.query(ApiKeyEntry.class, pageRequest)
                                                    .getResponse()
                                                    .stream()
                                                    .filter(apiKeyEntry -> checkpw(key, apiKeyEntry.getHashOfKey()))
                                                    .findFirst();
    ApiKeyEntry apiKeyEntry = apiKeyEntryOptional.orElse(null);
    if (apiKeyEntry == null) {
      return null;
    }

    return buildApiKeyEntry(apiKeyEntry.getUuid(), apiKeyEntry, details);
  }

  @Override
  public void deleteByAccountId(String accountId) {
    wingsPersistence.delete(
        wingsPersistence.createQuery(ApiKeyEntry.class).filter(ApiKeyEntryKeys.accountId, accountId));
  }
}