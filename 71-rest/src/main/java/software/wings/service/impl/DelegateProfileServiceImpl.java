package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.WingsException.USER;
import static io.harness.mongo.MongoUtils.setUnset;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Account;
import software.wings.beans.Delegate;
import software.wings.beans.Delegate.DelegateKeys;
import software.wings.beans.DelegateProfile;
import software.wings.beans.DelegateProfile.DelegateProfileKeys;
import software.wings.beans.Event;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.DelegateProfileService;
import software.wings.service.intfc.account.AccountCrudObserver;

import java.util.List;
import java.util.Optional;
import javax.validation.executable.ValidateOnExecution;

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

  @Override
  public PageResponse<DelegateProfile> list(PageRequest<DelegateProfile> pageRequest) {
    return wingsPersistence.query(DelegateProfile.class, pageRequest);
  }

  @Override
  public DelegateProfile get(String accountId, String delegateProfileId) {
    return wingsPersistence.createQuery(DelegateProfile.class)
        .filter(DelegateProfileKeys.uuid, delegateProfileId)
        .filter(DelegateProfileKeys.accountId, accountId)
        .get();
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
    UpdateOperations<DelegateProfile> updateOperations = wingsPersistence.createUpdateOperations(DelegateProfile.class);
    setUnset(updateOperations, DelegateProfileKeys.name, delegateProfile.getName());
    setUnset(updateOperations, DelegateProfileKeys.description, delegateProfile.getDescription());
    setUnset(updateOperations, DelegateProfileKeys.startupScript, delegateProfile.getStartupScript());
    setUnset(updateOperations, DelegateProfileKeys.approvalRequired, delegateProfile.isApprovalRequired());

    Query<DelegateProfile> query = wingsPersistence.createQuery(DelegateProfile.class)
                                       .filter(DelegateProfileKeys.accountId, delegateProfile.getAccountId())
                                       .filter(ID_KEY, delegateProfile.getUuid());
    wingsPersistence.update(query, updateOperations);
    DelegateProfile updatedDelegateProfile = get(delegateProfile.getAccountId(), delegateProfile.getUuid());
    logger.info("Updated delegate profile: {}", updatedDelegateProfile.getUuid());
    auditServiceHelper.reportForAuditingUsingAccountId(
        delegateProfile.getAccountId(), delegateProfile, updatedDelegateProfile, Event.Type.UPDATE);
    logger.info("Auditing update of Delegate Profile for accountId={}", delegateProfile.getAccountId());
    return updatedDelegateProfile;
  }

  @Override
  public DelegateProfile add(DelegateProfile delegateProfile) {
    wingsPersistence.save(delegateProfile);
    logger.info("Added delegate profile: {}", delegateProfile.getUuid());
    auditServiceHelper.reportForAuditingUsingAccountId(
        delegateProfile.getAccountId(), null, delegateProfile, Event.Type.CREATE);
    logger.info("Auditing adding of Delegate Profile for accountId={}", delegateProfile.getAccountId());
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
      logger.info("Deleting delegate profile: {}", delegateProfileId);
      wingsPersistence.delete(delegateProfile);
      auditServiceHelper.reportDeleteForAuditingUsingAccountId(delegateProfile.getAccountId(), delegateProfile);
      logger.info("Auditing deleting of Delegate Profile for accountId={}", delegateProfile.getAccountId());
    }
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
    logger.info("AccountCreated event received.");

    if (!account.isForImport()) {
      DelegateProfile delegateProfile = buildPrimaryDelegateProfile(account.getUuid());
      add(delegateProfile);

      logger.info("Primary Delegate Profile added.");

      return;
    }

    logger.info("Account is marked as ForImport and creation of Primary Delegate Profile has been skipped.");
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
