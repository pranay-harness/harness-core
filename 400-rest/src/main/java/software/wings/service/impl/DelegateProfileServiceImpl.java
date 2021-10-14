package software.wings.service.impl;

import static io.harness.annotations.dev.HarnessTeam.DEL;
import static io.harness.beans.FeatureName.PER_AGENT_CAPABILITIES;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.DELETE_ACTION;
import static io.harness.exception.WingsException.USER;
import static io.harness.mongo.MongoUtils.setUnset;
import static io.harness.persistence.HPersistence.returnNewOptions;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import io.harness.annotations.dev.BreakDependencyOn;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.Delegate.DelegateKeys;
import io.harness.delegate.beans.DelegateEntityOwner;
import io.harness.delegate.beans.DelegateInstanceStatus;
import io.harness.delegate.beans.DelegateProfile;
import io.harness.delegate.beans.DelegateProfile.DelegateProfileKeys;
import io.harness.delegate.beans.DelegateProfileScopingRule;
import io.harness.delegate.utils.DelegateEntityOwnerHelper;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.entity_crud.EntityChangeDTO;
import io.harness.eventsframework.producer.Message;
import io.harness.exception.InvalidRequestException;
import io.harness.ff.FeatureFlagService;
import io.harness.ng.core.utils.NGUtils;
import io.harness.observer.Subject;
import io.harness.persistence.DMSPersistence;
import io.harness.secrets.SecretService;
import io.harness.service.intfc.DelegateCache;
import io.harness.service.intfc.DelegateProfileObserver;

import software.wings.beans.Account;
import software.wings.beans.Event;
import software.wings.service.intfc.DelegateProfileService;
import software.wings.service.intfc.account.AccountCrudObserver;
import software.wings.utils.Utils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.protobuf.StringValue;
import io.dropwizard.jersey.validation.JerseyViolationException;
import io.fabric8.utils.Strings;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.validation.executable.ValidateOnExecution;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Singleton
@ValidateOnExecution
@Slf4j
@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
@BreakDependencyOn("software.wings.beans.Event")
@OwnedBy(DEL)
public class DelegateProfileServiceImpl implements DelegateProfileService, AccountCrudObserver {
  public static final String CG_PRIMARY_PROFILE_NAME = "Primary";
  public static final String NG_PRIMARY_PROFILE_NAME_TEMPLATE = "Primary %s Configuration";
  public static final String PRIMARY_PROFILE_DESCRIPTION = "The primary profile for the";

  // Command to use secrets in startUp script is ${secrets.getValue("secretName")}. Hence the pattern is like this.
  private static final Pattern secretNamePattern = Pattern.compile("\\$\\{secrets.getValue\\([^{}]+\\)}");

  @Inject private DMSPersistence persistence;
  @Inject private AuditServiceHelper auditServiceHelper;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private SecretService secretService;
  @Inject private DelegateCache delegateCache;
  @Inject @Named(EventsFrameworkConstants.ENTITY_CRUD) private Producer eventProducer;

  @Getter private final Subject<DelegateProfileObserver> delegateProfileSubject = new Subject<>();

  @Override
  public PageResponse<DelegateProfile> list(PageRequest<DelegateProfile> pageRequest) {
    return persistence.query(DelegateProfile.class, pageRequest);
  }

  @Override
  public DelegateProfile get(String accountId, String delegateProfileId) {
    return delegateCache.getDelegateProfile(accountId, delegateProfileId);
  }

  @Override
  public DelegateProfile getProfileByIdentifier(String accountId, DelegateEntityOwner owner, String profileIdentifier) {
    return persistence.createQuery(DelegateProfile.class)
        .filter(DelegateKeys.accountId, accountId)
        .filter(DelegateKeys.owner, owner)
        .filter(DelegateProfileKeys.identifier, profileIdentifier)
        .get();
  }

  @Override
  public DelegateProfile fetchCgPrimaryProfile(String accountId) {
    Optional<DelegateProfile> primaryProfile = Optional.ofNullable(
        persistence.createQuery(DelegateProfile.class)
            .filter(DelegateProfileKeys.accountId, accountId)
            .field(DelegateProfileKeys.ng)
            .notEqual(Boolean.TRUE) // This is required to cover case when flag is not set at all and when it is false
            .filter(DelegateProfileKeys.primary, Boolean.TRUE)
            .get());

    return primaryProfile.orElseGet(() -> add(buildPrimaryDelegateProfile(accountId, null, false)));
  }

  @Override
  public DelegateProfile fetchNgPrimaryProfile(final String accountId, @Nullable final DelegateEntityOwner owner) {
    Optional<DelegateProfile> primaryProfile =
        Optional.ofNullable(persistence.createQuery(DelegateProfile.class)
                                .filter(DelegateProfileKeys.accountId, accountId)
                                .filter(DelegateProfileKeys.ng, Boolean.TRUE)
                                .filter(DelegateProfileKeys.primary, Boolean.TRUE)
                                .filter(DelegateProfileKeys.owner, owner)
                                .get());

    return primaryProfile.orElseGet(() -> add(buildPrimaryDelegateProfile(accountId, owner, true)));
  }

  @Override
  public DelegateProfile update(DelegateProfile delegateProfile) {
    List<String> secretsName =
        findInvalidSecretsUsedInScript(delegateProfile.getAccountId(), delegateProfile.getStartupScript());
    if (isNotEmpty(secretsName)) {
      throw new InvalidRequestException(format(
          "Either secret[s] %s  don't exist or not scoped to account. Secrets used in delegate profile need to be scoped to account ",
          secretsName));
    }

    DelegateProfile originalProfile = get(delegateProfile.getAccountId(), delegateProfile.getUuid());

    UpdateOperations<DelegateProfile> updateOperations = persistence.createUpdateOperations(DelegateProfile.class);
    setUnset(updateOperations, DelegateProfileKeys.name, delegateProfile.getName());
    setUnset(updateOperations, DelegateProfileKeys.description, delegateProfile.getDescription());
    setUnset(updateOperations, DelegateProfileKeys.startupScript, delegateProfile.getStartupScript());
    setUnset(updateOperations, DelegateProfileKeys.approvalRequired, delegateProfile.isApprovalRequired());
    setUnset(updateOperations, DelegateProfileKeys.selectors, delegateProfile.getSelectors());
    setUnset(updateOperations, DelegateProfileKeys.scopingRules, delegateProfile.getScopingRules());

    Query<DelegateProfile> query = persistence.createQuery(DelegateProfile.class)
                                       .filter(DelegateProfileKeys.accountId, delegateProfile.getAccountId())
                                       .filter(ID_KEY, delegateProfile.getUuid());

    // Update and invalidate cache
    persistence.update(query, updateOperations);
    delegateCache.invalidateDelegateProfileCache(delegateProfile.getAccountId(), delegateProfile.getUuid());

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
  public DelegateProfile updateV2(DelegateProfile delegateProfile) {
    DelegateProfile originalProfile = getProfileByIdentifier(
        delegateProfile.getAccountId(), delegateProfile.getOwner(), delegateProfile.getIdentifier());

    UpdateOperations<DelegateProfile> updateOperations = persistence.createUpdateOperations(DelegateProfile.class);
    setUnset(updateOperations, DelegateProfileKeys.name, delegateProfile.getName());
    setUnset(updateOperations, DelegateProfileKeys.description, delegateProfile.getDescription());
    setUnset(updateOperations, DelegateProfileKeys.startupScript, delegateProfile.getStartupScript());
    setUnset(updateOperations, DelegateProfileKeys.approvalRequired, delegateProfile.isApprovalRequired());
    setUnset(updateOperations, DelegateProfileKeys.selectors, delegateProfile.getSelectors());
    setUnset(updateOperations, DelegateProfileKeys.scopingRules, delegateProfile.getScopingRules());

    Query<DelegateProfile> query = persistence.createQuery(DelegateProfile.class)
                                       .filter(DelegateProfileKeys.accountId, delegateProfile.getAccountId())
                                       .filter(DelegateProfileKeys.owner, delegateProfile.getOwner())
                                       .filter(DelegateProfileKeys.identifier, delegateProfile.getIdentifier());

    persistence.update(query, updateOperations);

    DelegateProfile updatedDelegateProfile = getProfileByIdentifier(
        delegateProfile.getAccountId(), delegateProfile.getOwner(), delegateProfile.getIdentifier());
    log.info("Updated delegate profile with identifier: {}", updatedDelegateProfile.getIdentifier());

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
    Query<DelegateProfile> delegateProfileQuery = persistence.createQuery(DelegateProfile.class)
                                                      .filter(DelegateProfileKeys.accountId, accountId)
                                                      .filter(DelegateProfileKeys.uuid, delegateProfileId);
    DelegateProfile originalProfile = delegateProfileQuery.get();

    UpdateOperations<DelegateProfile> updateOperations = persistence.createUpdateOperations(DelegateProfile.class);

    setUnset(updateOperations, DelegateProfileKeys.selectors, selectors);

    // Update and invalidate cache
    DelegateProfile delegateProfileSelectorsUpdated =
        persistence.findAndModify(delegateProfileQuery, updateOperations, returnNewOptions);
    delegateCache.invalidateDelegateProfileCache(accountId, delegateProfileId);
    log.info("Updated delegate profile selectors: {}", delegateProfileSelectorsUpdated.getSelectors());

    if (featureFlagService.isEnabled(PER_AGENT_CAPABILITIES, accountId)) {
      delegateProfileSubject.fireInform(
          DelegateProfileObxxxxxxxx:onProfileSelectorsUpdated, accountId, delegateProfileId);
    }

    auditServiceHelper.reportForAuditingUsingAccountId(
        accountId, originalProfile, delegateProfileSelectorsUpdated, Event.Type.UPDATE);
    log.info("Auditing update of Selectors of Delegate Profile for accountId={}", accountId);

    return delegateProfileSelectorsUpdated;
  }

  @Override
  public DelegateProfile updateProfileSelectorsV2(
      String accountId, DelegateEntityOwner owner, String delegateProfileIdentifier, List<String> selectors) {
    Query<DelegateProfile> delegateProfileQuery =
        persistence.createQuery(DelegateProfile.class)
            .filter(DelegateProfileKeys.accountId, accountId)
            .filter(DelegateProfileKeys.owner, owner)
            .filter(DelegateProfileKeys.identifier, delegateProfileIdentifier);
    DelegateProfile originalProfile = delegateProfileQuery.get();

    UpdateOperations<DelegateProfile> updateOperations = persistence.createUpdateOperations(DelegateProfile.class);

    setUnset(updateOperations, DelegateProfileKeys.selectors, selectors);

    // Update and invalidate cache
    DelegateProfile delegateProfileSelectorsUpdated =
        persistence.findAndModify(delegateProfileQuery, updateOperations, returnNewOptions);
    delegateCache.invalidateDelegateProfileCache(accountId, delegateProfileSelectorsUpdated.getUuid());
    log.info("Updated delegate profile selectors: {}", delegateProfileSelectorsUpdated.getSelectors());

    if (featureFlagService.isEnabled(PER_AGENT_CAPABILITIES, accountId)) {
      delegateProfileSubject.fireInform(
          DelegateProfileObxxxxxxxx:onProfileSelectorsUpdated, accountId, delegateProfileSelectorsUpdated.getUuid());
    }

    auditServiceHelper.reportForAuditingUsingAccountId(
        accountId, originalProfile, delegateProfileSelectorsUpdated, Event.Type.UPDATE);
    log.info("Auditing update of Selectors of Delegate Profile for accountId={}", accountId);

    return delegateProfileSelectorsUpdated;
  }

  @Override
  public DelegateProfile updateScopingRules(
      String accountId, String delegateProfileId, List<DelegateProfileScopingRule> scopingRules) {
    UpdateOperations<DelegateProfile> updateOperations = persistence.createUpdateOperations(DelegateProfile.class);
    setUnset(updateOperations, DelegateProfileKeys.scopingRules, scopingRules);
    Query<DelegateProfile> query = persistence.createQuery(DelegateProfile.class)
                                       .filter(DelegateProfileKeys.accountId, accountId)
                                       .filter(DelegateProfileKeys.uuid, delegateProfileId);
    // Update and invalidate cache
    DelegateProfile updatedDelegateProfile = persistence.findAndModify(query, updateOperations, returnNewOptions);
    delegateCache.invalidateDelegateProfileCache(accountId, delegateProfileId);
    log.info("Updated profile scoping rules for accountId={}", accountId);

    if (featureFlagService.isEnabled(PER_AGENT_CAPABILITIES, accountId)) {
      delegateProfileSubject.fireInform(DelegateProfileObxxxxxxxx:onProfileScopesUpdated, accountId, delegateProfileId);
    }

    return updatedDelegateProfile;
  }

  @Override
  public DelegateProfile updateScopingRules(String accountId, DelegateEntityOwner owner, String profileIdentifier,
      List<DelegateProfileScopingRule> scopingRules) {
    UpdateOperations<DelegateProfile> updateOperations = persistence.createUpdateOperations(DelegateProfile.class);
    setUnset(updateOperations, DelegateProfileKeys.scopingRules, scopingRules);
    Query<DelegateProfile> query = persistence.createQuery(DelegateProfile.class)
                                       .filter(DelegateProfileKeys.accountId, accountId)
                                       .filter(DelegateProfileKeys.owner, owner)
                                       .filter(DelegateProfileKeys.identifier, profileIdentifier);
    // Update and invalidate cache
    DelegateProfile updatedDelegateProfile = persistence.findAndModify(query, updateOperations, returnNewOptions);
    delegateCache.invalidateDelegateProfileCache(accountId, updatedDelegateProfile.getUuid());
    log.info("Updated profile scoping rules for accountId={}", accountId);

    if (featureFlagService.isEnabled(PER_AGENT_CAPABILITIES, accountId)) {
      delegateProfileSubject.fireInform(
          DelegateProfileObxxxxxxxx:onProfileScopesUpdated, accountId, updatedDelegateProfile.getUuid());
    }

    return updatedDelegateProfile;
  }

  @Override
  public DelegateProfile add(DelegateProfile delegateProfile) {
    if (Strings.isNullOrBlank(delegateProfile.getIdentifier())) {
      delegateProfile.setIdentifier(Utils.normalizeIdentifier(delegateProfile.getName()));
    }
    if (delegateProfile.isNg()) {
      try {
        NGUtils.validate(delegateProfile);
      } catch (JerseyViolationException exception) {
        throw new InvalidRequestException("Identifier " + delegateProfile.getIdentifier()
            + " did not pass validation checks: "
            + exception.getConstraintViolations()
                  .stream()
                  .map(i -> i.getMessage())
                  .reduce("", (i, j) -> i + " <" + j + "> "));
      }
    }
    if (Strings.isNotBlank(delegateProfile.getIdentifier())
        && identifierExists(
            delegateProfile.getAccountId(), delegateProfile.getOwner(), delegateProfile.getIdentifier())) {
      throw new InvalidRequestException(
          "The identifier already exists. Could not add delegate profile with identifier: "
          + delegateProfile.getIdentifier());
    }

    if (!delegateProfile.isNg()) {
      List<String> secretsName =
          findInvalidSecretsUsedInScript(delegateProfile.getAccountId(), delegateProfile.getStartupScript());
      if (isNotEmpty(secretsName)) {
        throw new InvalidRequestException(format(
            "Either secret[s] %s  don't exist or not scoped to account. Secrets used in delegate profile need to be scoped to account ",
            secretsName));
      }
    }

    persistence.save(delegateProfile);
    log.info("Added delegate profile: {}", delegateProfile.getUuid());
    auditServiceHelper.reportForAuditingUsingAccountId(
        delegateProfile.getAccountId(), null, delegateProfile, Event.Type.CREATE);
    log.info("Auditing adding of Delegate Profile for accountId={}", delegateProfile.getAccountId());
    return delegateProfile;
  }

  @Override
  public void delete(String accountId, String delegateProfileId) {
    DelegateProfile delegateProfile = persistence.createQuery(DelegateProfile.class)
                                          .filter(DelegateProfileKeys.accountId, accountId)
                                          .filter(ID_KEY, delegateProfileId)
                                          .get();
    if (delegateProfile != null) {
      ensureProfileSafeToDelete(accountId, delegateProfile);
      log.info("Deleting delegate profile: {}", delegateProfileId);
      // Delete and invalidate cache
      persistence.delete(delegateProfile);

      delegateCache.invalidateDelegateProfileCache(accountId, delegateProfileId);
      auditServiceHelper.reportDeleteForAuditingUsingAccountId(delegateProfile.getAccountId(), delegateProfile);
      log.info("Auditing deleting of Delegate Profile for accountId={}", delegateProfile.getAccountId());

      publishDelegateProfileChangeEventViaEventFramework(delegateProfile, DELETE_ACTION);
    }
  }

  @Override
  public void deleteProfileV2(String accountId, DelegateEntityOwner owner, String delegateProfileIdentifier) {
    DelegateProfile delegateProfile = persistence.createQuery(DelegateProfile.class)
                                          .filter(DelegateProfileKeys.accountId, accountId)
                                          .filter(DelegateProfileKeys.owner, owner)
                                          .filter(DelegateProfileKeys.identifier, delegateProfileIdentifier)
                                          .get();
    if (delegateProfile != null) {
      ensureProfileSafeToDelete(accountId, delegateProfile);
      log.info("Deleting delegate profile: {}", delegateProfile.getUuid());
      // Delete and invalidate cache
      persistence.delete(delegateProfile);

      delegateCache.invalidateDelegateProfileCache(accountId, delegateProfile.getUuid());
      auditServiceHelper.reportDeleteForAuditingUsingAccountId(delegateProfile.getAccountId(), delegateProfile);
      log.info("Auditing deleting of Delegate Profile for accountId={}", delegateProfile.getAccountId());

      publishDelegateProfileChangeEventViaEventFramework(delegateProfile, DELETE_ACTION);
    }
  }

  private void publishDelegateProfileChangeEventViaEventFramework(DelegateProfile delegateProfile, String action) {
    if (delegateProfile == null) {
      return;
    }

    try {
      EntityChangeDTO.Builder entityChangeDTOBuilder =
          EntityChangeDTO.newBuilder()
              .setAccountIdentifier(StringValue.of(delegateProfile.getAccountId()))
              .setIdentifier(StringValue.of(delegateProfile.getUuid()));

      if (delegateProfile.getOwner() != null) {
        String orgIdentifier =
            DelegateEntityOwnerHelper.extractOrgIdFromOwnerIdentifier(delegateProfile.getOwner().getIdentifier());
        if (isNotBlank(orgIdentifier)) {
          entityChangeDTOBuilder.setOrgIdentifier(StringValue.of(orgIdentifier));
        }

        String projectIdentifier =
            DelegateEntityOwnerHelper.extractProjectIdFromOwnerIdentifier(delegateProfile.getOwner().getIdentifier());
        if (isNotBlank(projectIdentifier)) {
          entityChangeDTOBuilder.setProjectIdentifier(StringValue.of(projectIdentifier));
        }
      }

      eventProducer.send(Message.newBuilder()
                             .putAllMetadata(ImmutableMap.of("accountId", delegateProfile.getAccountId(),
                                 EventsFrameworkMetadataConstants.ENTITY_TYPE,
                                 EventsFrameworkMetadataConstants.DELEGATE_CONFIGURATION_ENTITY,
                                 EventsFrameworkMetadataConstants.ACTION, action))
                             .setData(entityChangeDTOBuilder.build().toByteString())
                             .build());
    } catch (Exception ex) {
      log.error(String.format("Failed to publish delegate profile %s event for accountId %s via event framework.",
          action, delegateProfile.getAccountId()));
    }
  }

  @Override
  public void deleteByAccountId(String accountId) {
    persistence.delete(persistence.createQuery(DelegateProfile.class).filter(DelegateProfileKeys.accountId, accountId));
  }

  private void ensureProfileSafeToDelete(String accountId, DelegateProfile delegateProfile) {
    if (delegateProfile.isPrimary()) {
      throw new InvalidRequestException("Primary Delegate Profile cannot be deleted.", USER);
    }

    String delegateProfileId = delegateProfile.getUuid();
    List<Delegate> delegates = persistence.createQuery(Delegate.class)
                                   .filter(DelegateKeys.accountId, accountId)
                                   .field(DelegateKeys.status)
                                   .notEqual(DelegateInstanceStatus.DELETED)
                                   .asList();
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
      DelegateProfile cgDelegateProfile = buildPrimaryDelegateProfile(account.getUuid(), null, false);
      add(cgDelegateProfile);

      DelegateProfile ngDelegateProfile = buildPrimaryDelegateProfile(account.getUuid(), null, true);
      add(ngDelegateProfile);

      log.info("Primary Delegate Profiles added.");

      return;
    }

    log.info("Account is marked as ForImport and creation of Primary Delegate Profile has been skipped.");
  }

  @Override
  public void onAccountUpdated(Account account) {
    // Do nothing
  }

  @Override
  public List<String> getDelegatesForProfile(String accountId, String profileId) {
    return persistence.createQuery(Delegate.class)
        .filter(DelegateKeys.accountId, accountId)
        .filter(DelegateKeys.delegateProfileId, profileId)
        .field(DelegateKeys.status)
        .notEqual(DelegateInstanceStatus.DELETED)
        .asKeyList()
        .stream()
        .map(key -> key.getId().toString())
        .collect(toList());
  }

  private DelegateProfile buildPrimaryDelegateProfile(
      final String accountId, @Nullable final DelegateEntityOwner owner, final boolean isNg) {
    return DelegateProfile.builder()
        .uuid(generateUuid())
        .accountId(accountId)
        .name(getProfileName(owner, isNg))
        .description(getProfileDescription(owner, isNg))
        .primary(true)
        .owner(owner)
        .ng(isNg)
        .build();
  }

  private String getProfileName(final DelegateEntityOwner owner, final boolean isNg) {
    if (isNg) {
      if (DelegateEntityOwnerHelper.isAccount(owner)) {
        return String.format(NG_PRIMARY_PROFILE_NAME_TEMPLATE, "Account");
      } else if (DelegateEntityOwnerHelper.isOrganisation(owner)) {
        return String.format(NG_PRIMARY_PROFILE_NAME_TEMPLATE, "Organization");
      } else {
        return String.format(NG_PRIMARY_PROFILE_NAME_TEMPLATE, "Project");
      }
    } else {
      return CG_PRIMARY_PROFILE_NAME;
    }
  }

  private String getProfileDescription(final DelegateEntityOwner owner, final boolean isNg) {
    if (isNg) {
      if (DelegateEntityOwnerHelper.isAccount(owner)) {
        return String.format("%s %s", PRIMARY_PROFILE_DESCRIPTION, "account");
      } else if (DelegateEntityOwnerHelper.isOrganisation(owner)) {
        return String.format("%s %s organization", PRIMARY_PROFILE_DESCRIPTION, owner.getIdentifier());
      } else {
        return String.format("%s %s project", PRIMARY_PROFILE_DESCRIPTION, owner.getIdentifier());
      }
    } else {
      return String.format("%s %s", PRIMARY_PROFILE_DESCRIPTION, "account");
    }
  }

  @VisibleForTesting
  public boolean identifierExists(String accountId, DelegateEntityOwner owner, String proposedIdentifier) {
    Query<DelegateProfile> result = persistence.createQuery(DelegateProfile.class)
                                        .filter(DelegateKeys.accountId, accountId)
                                        .filter(DelegateKeys.owner, owner)
                                        .field(DelegateProfileKeys.identifier)
                                        .equalIgnoreCase(proposedIdentifier);
    return result.get() != null;
  }

  private List<String> findInvalidSecretsUsedInScript(String accountId, String script) {
    List<String> secretsName = new ArrayList<>();
    if (script == null) {
      return secretsName;
    }
    Matcher matcher = secretNamePattern.matcher(script);
    while (matcher.find()) {
      String secret =
          matcher.group(0).substring(matcher.group(0).indexOf("\"") + 1, matcher.group(0).lastIndexOf("\""));
      if (!secretService.getAccountScopedSecretByName(accountId, secret).isPresent()) {
        secretsName.add(secret);
      }
    }
    return secretsName;
  }
}
