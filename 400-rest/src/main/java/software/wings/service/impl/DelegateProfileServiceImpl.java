package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.WingsException.USER;
import static io.harness.mongo.MongoUtils.setUnset;
import static io.harness.persistence.HPersistence.returnNewOptions;

import static com.google.common.cache.CacheLoader.InvalidCacheLoadException;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.delegate.beans.DelegateProfile;
import io.harness.delegate.beans.DelegateProfile.DelegateProfileKeys;
import io.harness.delegate.beans.DelegateProfileScopingRule;
import io.harness.exception.InvalidRequestException;
import io.harness.observer.Subject;
import io.harness.service.intfc.DelegateProfileObserver;

import software.wings.beans.Account;
import software.wings.beans.Delegate;
import software.wings.beans.Delegate.DelegateKeys;
import software.wings.beans.Event;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.DelegateProfileService;
import software.wings.service.intfc.account.AccountCrudObserver;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.validation.executable.ValidateOnExecution;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

/**
 * Created by rishi on 7/31/18
 */
@Singleton
@ValidateOnExecution
@Slf4j
public class DelegateProfileServiceImpl implements DelegateProfileService, AccountCrudObserver {
  public static final String PRIMARY_PROFILE_NAME = "Primary";
  public static final String PRIMARY_PROFILE_DESCRIPTION = "The primary profile for the account";

  @Inject private WingsPersistence wingsPersistence;
  @Inject private AuditServiceHelper auditServiceHelper;

  @Getter private Subject<DelegateProfileObserver> delegateProfileSubject = new Subject<>();

  private LoadingCache<ImmutablePair<String, String>, DelegateProfile> delegateProfilesCache =
      CacheBuilder.newBuilder()
          .maximumSize(10000)
          .expireAfterWrite(5, TimeUnit.SECONDS)
          .build(new CacheLoader<ImmutablePair<String, String>, DelegateProfile>() {
            @Override
            public DelegateProfile load(ImmutablePair<String, String> delegateProfileKey) {
              return wingsPersistence.createQuery(DelegateProfile.class)
                  .filter(DelegateProfileKeys.accountId, delegateProfileKey.getLeft())
                  .filter(DelegateProfileKeys.uuid, delegateProfileKey.getRight())
                  .get();
            }
          });

  @Override
  public PageResponse<DelegateProfile> list(PageRequest<DelegateProfile> pageRequest) {
    return wingsPersistence.query(DelegateProfile.class, pageRequest);
  }

  @Override
  public DelegateProfile get(String accountId, String delegateProfileId) {
    if (StringUtils.isBlank(delegateProfileId)) {
      return null;
    }

    try {
      return delegateProfilesCache.get(ImmutablePair.of(accountId, delegateProfileId));
    } catch (ExecutionException | InvalidCacheLoadException e) {
      return null;
    }
  }

  @Override
  public DelegateProfile fetchPrimaryProfile(String accountId) {
    Optional<DelegateProfile> primaryProfile =
        Optional.ofNullable(wingsPersistence.createQuery(DelegateProfile.class)
                                .filter(DelegateProfileKeys.primary, Boolean.TRUE)
                                .filter(DelegateProfileKeys.accountId, accountId)
                                .get());

    return primaryProfile.orElseGet(() -> add(buildPrimaryDelegateProfile(accountId)));
  }

  @Override
  public DelegateProfile update(DelegateProfile delegateProfile) {
    DelegateProfile originalProfile = get(delegateProfile.getAccountId(), delegateProfile.getUuid());

    UpdateOperations<DelegateProfile> updateOperations = wingsPersistence.createUpdateOperations(DelegateProfile.class);
    setUnset(updateOperations, DelegateProfileKeys.name, delegateProfile.getName());
    setUnset(updateOperations, DelegateProfileKeys.description, delegateProfile.getDescription());
    setUnset(updateOperations, DelegateProfileKeys.startupScript, delegateProfile.getStartupScript());
    setUnset(updateOperations, DelegateProfileKeys.approvalRequired, delegateProfile.isApprovalRequired());
    setUnset(updateOperations, DelegateProfileKeys.selectors, delegateProfile.getSelectors());
    setUnset(updateOperations, DelegateProfileKeys.scopingRules, delegateProfile.getScopingRules());

    Query<DelegateProfile> query = wingsPersistence.createQuery(DelegateProfile.class)
                                       .filter(DelegateProfileKeys.accountId, delegateProfile.getAccountId())
                                       .filter(ID_KEY, delegateProfile.getUuid());

    // Update and invalidate cache
    wingsPersistence.update(query, updateOperations);
    delegateProfilesCache.invalidate(ImmutablePair.of(delegateProfile.getAccountId(), delegateProfile.getUuid()));

    DelegateProfile updatedDelegateProfile = get(delegateProfile.getAccountId(), delegateProfile.getUuid());
    log.info("Updated delegate profile: {}", updatedDelegateProfile.getUuid());

    delegateProfileSubject.fireInform(
        DelegateProfileObxxxxxxxx:onProfileUpdated, originalProfile, updatedDelegateProfile);

    auditServiceHelper.reportForAuditingUsingAccountId(
        delegateProfile.getAccountId(), delegateProfile, updatedDelegateProfile, Event.Type.UPDATE);
    log.info("Auditing update of Delegate Profile for accountId={}", delegateProfile.getAccountId());
    return updatedDelegateProfile;
  }

  @Override
  public DelegateProfile updateDelegateProfileSelectors(
      String delegateProfileId, String accountId, List<String> selectors) {
    Query<DelegateProfile> delegateProfileQuery = wingsPersistence.createQuery(DelegateProfile.class)
                                                      .filter(DelegateProfileKeys.accountId, accountId)
                                                      .filter(DelegateProfileKeys.uuid, delegateProfileId);
    UpdateOperations<DelegateProfile> updateOperations = wingsPersistence.createUpdateOperations(DelegateProfile.class);

    setUnset(updateOperations, DelegateProfileKeys.selectors, selectors);

    // Update and invalidate cache
    DelegateProfile delegateProfileSelectorsUpdated =
        wingsPersistence.findAndModify(delegateProfileQuery, updateOperations, returnNewOptions);
    delegateProfilesCache.invalidate(ImmutablePair.of(accountId, delegateProfileId));
    log.info("Updated delegate profile selectors: {}", delegateProfileSelectorsUpdated.getSelectors());
    return delegateProfileSelectorsUpdated;
  }

  @Override
  public DelegateProfile updateScopingRules(
      String accountId, String delegateProfileId, List<DelegateProfileScopingRule> scopingRules) {
    UpdateOperations<DelegateProfile> updateOperations = wingsPersistence.createUpdateOperations(DelegateProfile.class);
    setUnset(updateOperations, DelegateProfileKeys.scopingRules, scopingRules);
    Query<DelegateProfile> query = wingsPersistence.createQuery(DelegateProfile.class)
                                       .filter(DelegateProfileKeys.accountId, accountId)
                                       .filter(DelegateProfileKeys.uuid, delegateProfileId);
    // Update and invalidate cache
    DelegateProfile updatedDelegateProfile = wingsPersistence.findAndModify(query, updateOperations, returnNewOptions);
    delegateProfilesCache.invalidate(ImmutablePair.of(accountId, delegateProfileId));
    log.info("Updated profile scoping rules for accountId={}", accountId);
    return updatedDelegateProfile;
  }

  @Override
  public DelegateProfile add(DelegateProfile delegateProfile) {
    wingsPersistence.save(delegateProfile);
    log.info("Added delegate profile: {}", delegateProfile.getUuid());
    auditServiceHelper.reportForAuditingUsingAccountId(
        delegateProfile.getAccountId(), null, delegateProfile, Event.Type.CREATE);
    log.info("Auditing adding of Delegate Profile for accountId={}", delegateProfile.getAccountId());
    return delegateProfile;
  }

  @Override
  public void delete(String accountId, String delegateProfileId) {
    DelegateProfile delegateProfile = wingsPersistence.createQuery(DelegateProfile.class)
                                          .filter(DelegateProfileKeys.accountId, accountId)
                                          .filter(ID_KEY, delegateProfileId)
                                          .get();
    if (delegateProfile != null) {
      ensureProfileSafeToDelete(accountId, delegateProfile);
      log.info("Deleting delegate profile: {}", delegateProfileId);
      // Delete and invalidate cache
      wingsPersistence.delete(delegateProfile);
      delegateProfilesCache.invalidate(ImmutablePair.of(accountId, delegateProfileId));

      auditServiceHelper.reportDeleteForAuditingUsingAccountId(delegateProfile.getAccountId(), delegateProfile);
      log.info("Auditing deleting of Delegate Profile for accountId={}", delegateProfile.getAccountId());
    }
  }

  @Override
  public void deleteByAccountId(String accountId) {
    wingsPersistence.delete(
        wingsPersistence.createQuery(DelegateProfile.class).filter(DelegateProfileKeys.accountId, accountId));
  }

  private void ensureProfileSafeToDelete(String accountId, DelegateProfile delegateProfile) {
    if (delegateProfile.isPrimary()) {
      throw new InvalidRequestException("Primary Delegate Profile cannot be deleted.", USER);
    }

    String delegateProfileId = delegateProfile.getUuid();
    List<Delegate> delegates =
        wingsPersistence.createQuery(Delegate.class).filter(DelegateKeys.accountId, accountId).asList();
    List<String> delegateNames = delegates.stream()
                                     .filter(delegate -> delegateProfileId.equals(delegate.getDelegateProfileId()))
                                     .map(Delegate::getHostName)
                                     .collect(toList());
    if (isNotEmpty(delegateNames)) {
      String message = format("Delegate profile [%s] could not be deleted because it's used by these delegates [%s]",
          delegateProfile.getName(), String.join(", ", delegateNames));
      throw new InvalidRequestException(message, USER);
    }
  }

  @Override
  public void onAccountCreated(Account account) {
    log.info("AccountCreated event received.");

    if (!account.isForImport()) {
      DelegateProfile delegateProfile = buildPrimaryDelegateProfile(account.getUuid());
      add(delegateProfile);

      log.info("Primary Delegate Profile added.");

      return;
    }

    log.info("Account is marked as ForImport and creation of Primary Delegate Profile has been skipped.");
  }

  @Override
  public void onAccountUpdated(Account account) {
    // Do nothing
  }

  private DelegateProfile buildPrimaryDelegateProfile(String accountId) {
    return DelegateProfile.builder()
        .uuid(generateUuid())
        .accountId(accountId)
        .name(PRIMARY_PROFILE_NAME)
        .description(PRIMARY_PROFILE_DESCRIPTION)
        .primary(true)
        .build();
  }
}
