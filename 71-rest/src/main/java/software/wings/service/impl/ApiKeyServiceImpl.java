package software.wings.service.impl;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.beans.SearchFilter.Operator.HAS;
import static io.harness.beans.SearchFilter.Operator.IN;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.WingsException.USER;
import static io.harness.mongo.MongoUtils.setUnset;
import static io.harness.validation.Validator.notNullCheck;
import static java.lang.System.currentTimeMillis;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
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
import io.harness.persistence.HQuery;
import io.harness.security.SimpleEncryption;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.mindrot.jbcrypt.BCrypt;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Account;
import software.wings.beans.ApiKeyEntry;
import software.wings.beans.ApiKeyEntry.ApiKeyEntryKeys;
import software.wings.beans.Base;
import software.wings.beans.Event.Type;
import software.wings.beans.security.UserGroup;
import software.wings.dl.WingsPersistence;
import software.wings.features.ApiKeysFeature;
import software.wings.features.api.RestrictedApi;
import software.wings.security.UserPermissionInfo;
import software.wings.security.UserRestrictionInfo;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.ApiKeyService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.UserGroupService;
import software.wings.utils.CacheManager;
import software.wings.utils.CryptoUtils;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import javax.cache.Cache;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
@Slf4j
public class ApiKeyServiceImpl implements ApiKeyService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AccountService accountService;
  @Inject private UserGroupService userGroupService;
  @Inject private CacheManager cacheManager;
  @Inject private AuthHandler authHandler;
  @Inject private AuthService authService;
  @Inject private ExecutorService executorService;
  @Inject private AuditServiceHelper auditServiceHelper;

  private static String DELIMITER = "::";

  private SimpleEncryption getSimpleEncryption(String accountId) {
    Account account = accountService.get(accountId);
    notNullCheck("Account is null for accountId: " + accountId, account);
    return new SimpleEncryption(account.getAccountKey().toCharArray());
  }

  @Override
  @RestrictedApi(ApiKeysFeature.class)
  public ApiKeyEntry generate(String accountId, ApiKeyEntry apiKeyEntry) {
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
    auditServiceHelper.reportForAuditingUsingAccountId(accountId, null, apiKeyEntry, Type.CREATE);
    return get(id, accountId);
  }

  @Override
  @RestrictedApi(ApiKeysFeature.class)
  public ApiKeyEntry update(String uuid, String accountId, ApiKeyEntry apiKeyEntry) {
    notNullCheck("ApiKeyEntry is null", apiKeyEntry, USER);
    notNullCheck("uuid is null for the given api key entry", uuid, USER);
    notNullCheck(UserGroup.ACCOUNT_ID_KEY, accountId, USER);

    ApiKeyEntry apiKeyEntryBeforeUpdate = get(uuid, accountId);

    UpdateOperations<ApiKeyEntry> operations = wingsPersistence.createUpdateOperations(ApiKeyEntry.class);
    setUnset(operations, "name", apiKeyEntry.getName());
    setUnset(operations, "userGroupIds", apiKeyEntry.getUserGroupIds());

    Query<ApiKeyEntry> query = wingsPersistence.createQuery(ApiKeyEntry.class)
                                   .filter(ID_KEY, uuid)
                                   .filter(UserGroup.ACCOUNT_ID_KEY, accountId);
    wingsPersistence.update(query, operations);
    ApiKeyEntry apiKeyEntryAfterUpdate = get(uuid, accountId);
    boolean same =
        ListUtils.isEqualList(apiKeyEntryBeforeUpdate.getUserGroupIds(), apiKeyEntryAfterUpdate.getUserGroupIds());
    if (!same) {
      evictApiKeyAndRebuildCache(apiKeyEntryAfterUpdate.getDecryptedKey(), accountId, true);
    }
    auditServiceHelper.reportForAuditingUsingAccountId(accountId, null, apiKeyEntry, Type.UPDATE);
    return apiKeyEntryAfterUpdate;
  }

  private void evictApiKeyAndRebuildCache(String apiKey, String accountId, boolean rebuild) {
    Cache<String, ApiKeyEntry> apiKeyCache = requireNonNull(cacheManager.getApiKeyCache());
    Cache<String, UserPermissionInfo> apiKeyPermissionInfoCache =
        requireNonNull(cacheManager.getApiKeyPermissionInfoCache());
    Cache<String, UserRestrictionInfo> apiKeyRestrictionInfoCache =
        requireNonNull(cacheManager.getApiKeyRestrictionInfoCache());

    boolean apiKeyPresent = apiKeyCache.remove(apiKey);
    boolean apiKeyPresentInPermissions = apiKeyPermissionInfoCache.remove(apiKey);
    boolean apiKeyPresentInRestrictions = apiKeyRestrictionInfoCache.remove(apiKey);

    if (rebuild) {
      executorService.submit(() -> {
        if (apiKeyPresent) {
          // This call gets the permissions from cache, if present.
          getApiKeyFromCacheOrDB(apiKey, accountId, true);
        }

        if (apiKeyPresentInPermissions || apiKeyPresentInRestrictions) {
          rebuildPermissionsAndRestrictions(apiKey, accountId);
        }
      });
    }
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
        allUserGroupList.stream().collect(Collectors.toMap(Base::getUuid, identity()));

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
  public PageResponse<ApiKeyEntry> list(
      PageRequest<ApiKeyEntry> pageRequest, String accountId, boolean loadUserGroups, boolean decrypt) {
    pageRequest.addFilter("accountId", EQ, accountId);
    PageResponse<ApiKeyEntry> response = wingsPersistence.query(ApiKeyEntry.class, pageRequest, HQuery.excludeValidate);
    if (response.isEmpty()) {
      return response;
    }

    List<ApiKeyEntry> apiKeyEntryList = response.getResponse();
    if (decrypt) {
      apiKeyEntryList.forEach(apiKeyEntry -> {
        String decryptedKey =
            new String(getSimpleEncryption(apiKeyEntry.getAccountId()).decryptChars(apiKeyEntry.getEncryptedKey()));
        apiKeyEntry.setDecryptedKey(decryptedKey);
      });
    }
    if (loadUserGroups) {
      loadUserGroupsForApiKeys(apiKeyEntryList, accountId);
    }
    return response;
  }

  @Override
  public ApiKeyEntry get(String uuid, String accountId) {
    ApiKeyEntry entry = wingsPersistence.createQuery(ApiKeyEntry.class)
                            .filter(ApiKeyEntryKeys.accountId, accountId)
                            .filter(ID_KEY, uuid)
                            .get();
    return buildApiKeyEntry(uuid, entry, true);
  }

  private ApiKeyEntry buildApiKeyEntry(String uuid, ApiKeyEntry entry, boolean details) {
    notNullCheck("apiKeyEntry is null for id: " + uuid, entry);
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
    if (apiKey.contains(DELIMITER)) {
      String decodedApiKey = new String(Base64.getDecoder().decode(apiKey), Charsets.UTF_8);
      String[] split = decodedApiKey.split(DELIMITER);
      return split[0];
    } else {
      return null;
    }
  }

  @Override
  public void delete(String accountId, String uuid) {
    ApiKeyEntry apiKeyEntry = get(uuid, accountId);
    evictApiKeyAndRebuildCache(apiKeyEntry.getDecryptedKey(), accountId, false);
    auditServiceHelper.reportDeleteForAuditingUsingAccountId(accountId, apiKeyEntry);
    wingsPersistence.delete(ApiKeyEntry.class, uuid);
  }

  @Override
  public void validate(String apiKey, String accountId) {
    ApiKeyEntry apiKeyEntry = getApiKeyFromCacheOrDB(apiKey, accountId, true);
    if (apiKeyEntry == null) {
      throw new UnauthorizedException("Invalid Api Key", USER);
    }
  }

  @Override
  public ApiKeyEntry getByKey(String key, String accountId, boolean details) {
    return getApiKeyFromCacheOrDB(key, accountId, details);
  }

  private ApiKeyEntry getByKeyFromDB(String key, String accountId, boolean details) {
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

  private ApiKeyEntry getApiKeyFromCacheOrDB(String apiKey, String accountId, boolean details) {
    Cache<String, ApiKeyEntry> apiKeyCache = cacheManager.getApiKeyCache();
    if (apiKeyCache == null) {
      logger.warn("apiKeyCache is null. Fetch from DB");
      return getByKeyFromDB(apiKey, accountId, details);
    } else {
      ApiKeyEntry apiKeyEntry;
      try {
        apiKeyEntry = apiKeyCache.get(apiKey);
        if (apiKeyEntry == null) {
          apiKeyEntry = getByKeyFromDB(apiKey, accountId, details);
          notNullCheck("Api-key does not exist", apiKeyEntry, USER);
          apiKeyCache.put(apiKeyEntry.getDecryptedKey(), apiKeyEntry);
        }
      } catch (Exception ex) {
        // If there was any exception, remove that entry from cache
        apiKeyCache.remove(apiKey);
        apiKeyEntry = getByKeyFromDB(apiKey, accountId, details);
        if (apiKeyEntry != null) {
          apiKeyCache.put(apiKeyEntry.getDecryptedKey(), apiKeyEntry);
        }
      }
      return apiKeyEntry;
    }
  }

  @Override
  public UserPermissionInfo getApiKeyPermissions(ApiKeyEntry apiKeyEntry, String accountId) {
    String apiKey = apiKeyEntry.getDecryptedKey();
    Cache<String, UserPermissionInfo> apiKeyPermissionsCache = cacheManager.getApiKeyPermissionInfoCache();
    if (apiKeyPermissionsCache == null) {
      logger.warn("apiKey permissions cache is null. Fetch from DB");
      return authHandler.evaluateUserPermissionInfo(accountId, apiKeyEntry.getUserGroups(), null);
    } else {
      UserPermissionInfo apiKeyPermissionInfo;
      try {
        apiKeyPermissionInfo = apiKeyPermissionsCache.get(apiKey);
        if (apiKeyPermissionInfo == null) {
          apiKeyPermissionInfo = authHandler.evaluateUserPermissionInfo(accountId, apiKeyEntry.getUserGroups(), null);
          apiKeyPermissionsCache.put(apiKey, apiKeyPermissionInfo);
        }
      } catch (Exception ex) {
        // If there was any exception, remove that entry from cache
        apiKeyPermissionsCache.remove(apiKey);
        apiKeyPermissionInfo = authHandler.evaluateUserPermissionInfo(accountId, apiKeyEntry.getUserGroups(), null);
        apiKeyPermissionsCache.put(apiKey, apiKeyPermissionInfo);
      }
      return apiKeyPermissionInfo;
    }
  }

  @Override
  public UserRestrictionInfo getApiKeyRestrictions(
      ApiKeyEntry apiKeyEntry, UserPermissionInfo userPermissionInfo, String accountId) {
    String apiKey = apiKeyEntry.getDecryptedKey();
    Cache<String, UserRestrictionInfo> apiKeyRestrictionInfoCache = cacheManager.getApiKeyRestrictionInfoCache();
    if (apiKeyRestrictionInfoCache == null) {
      logger.warn("apiKey restrictions cache is null. Fetch from DB");
      return authService.getUserRestrictionInfoFromDB(accountId, userPermissionInfo, apiKeyEntry.getUserGroups());
    } else {
      UserRestrictionInfo apiKeyPermissionInfo;
      try {
        apiKeyPermissionInfo = apiKeyRestrictionInfoCache.get(apiKey);
        if (apiKeyPermissionInfo == null) {
          apiKeyPermissionInfo =
              authService.getUserRestrictionInfoFromDB(accountId, userPermissionInfo, apiKeyEntry.getUserGroups());
          apiKeyRestrictionInfoCache.put(apiKey, apiKeyPermissionInfo);
        }
      } catch (Exception ex) {
        // If there was any exception, remove that entry from cache
        apiKeyRestrictionInfoCache.remove(apiKey);
        apiKeyPermissionInfo =
            authService.getUserRestrictionInfoFromDB(accountId, userPermissionInfo, apiKeyEntry.getUserGroups());
        apiKeyRestrictionInfoCache.put(apiKey, apiKeyPermissionInfo);
      }
      return apiKeyPermissionInfo;
    }
  }

  @Override
  public void deleteByAccountId(String accountId) {
    PageResponse<ApiKeyEntry> pageResponse = list(PageRequestBuilder.aPageRequest().build(), accountId, false, true);
    List<ApiKeyEntry> apiKeyEntryList = pageResponse.getResponse();
    wingsPersistence.delete(
        wingsPersistence.createQuery(ApiKeyEntry.class).filter(ApiKeyEntryKeys.accountId, accountId));

    apiKeyEntryList.forEach(apiKeyEntry -> evictApiKeyAndRebuildCache(apiKeyEntry.getDecryptedKey(), accountId, false));
  }

  @Override
  public void evictAndRebuildPermissions(String accountId, boolean rebuild) {
    Cache<String, UserPermissionInfo> permissionsCache = cacheManager.getApiKeyPermissionInfoCache();

    PageResponse pageResponse = list(PageRequestBuilder.aPageRequest().build(), accountId, false, true);
    List<ApiKeyEntry> apiKeyEntryList = pageResponse.getResponse();
    if (isEmpty(apiKeyEntryList)) {
      return;
    }

    Set<String> keys = new HashSet<>();

    apiKeyEntryList.forEach(apiKeyEntry -> {
      String apiKey = apiKeyEntry.getDecryptedKey();
      boolean hasPermissions = permissionsCache.remove(apiKey);
      if (hasPermissions) {
        keys.add(apiKey);
      }
    });

    if (rebuild) {
      executorService.submit(() -> keys.forEach(key -> {
        ApiKeyEntry apiKeyEntry = get(key, accountId);
        // rebuild cache
        getApiKeyPermissions(apiKeyEntry, accountId);
      }));
    }
  }

  @Override
  public void evictAndRebuildPermissionsAndRestrictions(String accountId, boolean rebuild) {
    PageResponse pageResponse = list(PageRequestBuilder.aPageRequest().build(), accountId, false, true);
    List<ApiKeyEntry> apiKeyEntryList = pageResponse.getResponse();
    evictAndRebuildPermissionsAndRestrictions(accountId, rebuild, apiKeyEntryList);
  }

  private void evictAndRebuildPermissionsAndRestrictions(
      String accountId, boolean rebuild, List<ApiKeyEntry> apiKeyEntryList) {
    if (isEmpty(apiKeyEntryList)) {
      return;
    }

    apiKeyEntryList.forEach(
        apiKeyEntry -> evictApiKeyAndRebuildCache(apiKeyEntry.getDecryptedKey(), accountId, rebuild));
  }

  private void rebuildPermissionsAndRestrictions(String apiKey, String accountId) {
    ApiKeyEntry apiKeyEntry = getByKey(apiKey, accountId, true);
    if (apiKeyEntry != null) {
      rebuildPermissionsAndRestrictions(apiKeyEntry);
    }
  }

  private void rebuildPermissionsAndRestrictions(ApiKeyEntry apiKeyEntry) {
    String accountId = apiKeyEntry.getAccountId();
    // Load cache again
    UserPermissionInfo permissions = getApiKeyPermissions(apiKeyEntry, accountId);
    getApiKeyRestrictions(apiKeyEntry, permissions, accountId);
  }

  @Override
  public void evictPermissionsAndRestrictionsForUserGroup(UserGroup userGroup) {
    PageRequest<ApiKeyEntry> pageRequest =
        PageRequestBuilder.aPageRequest().addFilter("userGroupIds", HAS, userGroup.getUuid()).build();
    PageResponse pageResponse = list(pageRequest, userGroup.getAccountId(), false, true);
    List<ApiKeyEntry> apiKeyEntryList = pageResponse.getResponse();
    evictAndRebuildPermissionsAndRestrictions(userGroup.getAccountId(), true, apiKeyEntryList);
  }
}