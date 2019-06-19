package software.wings.service.impl;

import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.mongo.MongoUtils.setUnset;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.AccountAuditFilter;
import software.wings.beans.ApplicationAuditFilter;
import software.wings.beans.AuditPreference;
import software.wings.beans.AuditPreference.AuditPreferenceKeys;
import software.wings.beans.AuditPreferenceResponse;
import software.wings.beans.AuditPreferenceResponse.AuditPreferenceResponseBuilder;
import software.wings.beans.DeploymentPreference;
import software.wings.beans.Preference;
import software.wings.beans.Preference.PreferenceKeys;
import software.wings.beans.PreferenceType;
import software.wings.beans.ResourceLookup;
import software.wings.beans.ResourceLookup.ResourceLookupKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.PreferenceService;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Singleton
@Slf4j
public class PreferenceServiceImpl implements PreferenceService {
  public static final String USER_ID_KEY = "userId";
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public Preference save(String accountId, String userId, Preference preference) {
    Preference savedPreference = null;
    Preference existingPreference = getPreferenceByName(accountId, userId, preference.getName());
    if (existingPreference != null) {
      throw new InvalidRequestException("Preference with same name exists for user", USER)
          .addParam("message", "Preference with same name exists for user");
    }

    savedPreference = wingsPersistence.saveAndGet(Preference.class, preference);
    return savedPreference;
  }

  @Override
  public Preference getPreferenceByName(String accountId, String userId, String name) {
    return wingsPersistence.createQuery(Preference.class)
        .filter(PreferenceKeys.accountId, accountId)
        .filter(PreferenceKeys.userId, userId)
        .filter(PreferenceKeys.name, name)
        .get();
  }

  @Override
  public Preference get(String accountId, String userId, String preferenceId) {
    return wingsPersistence.createQuery(Preference.class)
        .filter(PreferenceKeys.accountId, accountId)
        .filter(USER_ID_KEY, userId)
        .filter(PreferenceKeys.uuid, preferenceId)
        .get();
  }

  @Override
  public PageResponse<Preference> list(PageRequest<Preference> pageRequest, String userId) {
    pageRequest.addFilter(USER_ID_KEY, EQ, userId);
    return wingsPersistence.query(Preference.class, pageRequest);
  }

  @Override
  public AuditPreferenceResponse listAuditPreferences(String accountId, String userId) {
    // pageRequest.addFilter(USER_ID_KEY, EQ, userId);
    List<AuditPreference> auditPreferences =
        wingsPersistence.createQuery(AuditPreference.class)
            .filter(AuditPreferenceKeys.accountId, accountId)
            .filter(AuditPreferenceKeys.preferenceType, PreferenceType.AUDIT_PREFERENCE.name())
            .filter(AuditPreferenceKeys.userId, userId)
            .asList();

    AuditPreferenceResponseBuilder responseBuilder =
        AuditPreferenceResponse.builder().auditPreferences(auditPreferences);

    generateResourceLookupsForIdsInFilter(auditPreferences, responseBuilder, accountId);

    return responseBuilder.build();
  }

  private void generateResourceLookupsForIdsInFilter(
      List<AuditPreference> auditPreferences, AuditPreferenceResponseBuilder responseBuilder, String accountId) {
    Set<String> ids = new HashSet<>();

    // generate Id set
    auditPreferences.forEach(auditPreference -> {
      ApplicationAuditFilter applicationAuditFilter = auditPreference.getApplicationAuditFilter();
      if (applicationAuditFilter != null) {
        addToSet(ids, applicationAuditFilter.getAppIds());
        addToSet(ids, applicationAuditFilter.getResourceIds());
      }

      AccountAuditFilter accountAuditFilter = auditPreference.getAccountAuditFilter();
      if (accountAuditFilter != null) {
        addToSet(ids, accountAuditFilter.getResourceIds());
      }
    });

    List<ResourceLookup> resourceLookups = wingsPersistence.createQuery(ResourceLookup.class)
                                               .field(ResourceLookupKeys.resourceId)
                                               .in(ids)
                                               .filter(ResourceLookupKeys.accountId, accountId)
                                               .project(ResourceLookupKeys.resourceId, true)
                                               .project(ResourceLookupKeys.resourceName, true)
                                               .project(ResourceLookupKeys.resourceType, true)
                                               .project(ResourceLookupKeys.appId, true)
                                               .project(ResourceLookupKeys.accountId, true)
                                               .asList();

    Map<String, ResourceLookup> lookupMap = new HashMap<>();
    if (isNotEmpty(resourceLookups)) {
      resourceLookups.forEach(resourceLookup -> lookupMap.put(resourceLookup.getResourceId(), resourceLookup));
    }

    responseBuilder.resourceLookupMap(lookupMap);
  }

  private void addToSet(Set<String> ids, List<String> input) {
    if (isNotEmpty(input)) {
      ids.addAll(input);
    }
  }

  @Override
  public Preference update(String accountId, String userId, String preferenceId, Preference preference) {
    // Update preference for given account, user and preference Id

    if (preference instanceof DeploymentPreference) {
      DeploymentPreference deployPref = null;
      deployPref = (DeploymentPreference) preference;

      UpdateOperations<Preference> updateOperations = wingsPersistence.createUpdateOperations(Preference.class);
      // Set fields to update
      setUnset(updateOperations, "name", deployPref.getName());
      setUnset(updateOperations, "appIds", deployPref.getAppIds());
      setUnset(updateOperations, "pipelineIds", deployPref.getPipelineIds());
      setUnset(updateOperations, "workflowIds", deployPref.getWorkflowIds());
      setUnset(updateOperations, "serviceIds", deployPref.getServiceIds());
      setUnset(updateOperations, "envIds", deployPref.getEnvIds());
      setUnset(updateOperations, "status", deployPref.getStatus());
      setUnset(updateOperations, "startTime", deployPref.getStartTime());
      setUnset(updateOperations, "endTime", deployPref.getEndTime());
      setUnset(updateOperations, "keywords", deployPref.getKeywords());

      update(accountId, userId, preferenceId, updateOperations);
    } else if (preference instanceof AuditPreference) {
      AuditPreference auditPreference = (AuditPreference) preference;

      UpdateOperations<Preference> updateOperations = wingsPersistence.createUpdateOperations(Preference.class);
      // Set fields to update
      setUnset(updateOperations, AuditPreferenceKeys.name, auditPreference.getName());
      setUnset(updateOperations, AuditPreferenceKeys.startTime, auditPreference.getStartTime());
      setUnset(updateOperations, AuditPreferenceKeys.endTime, auditPreference.getEndTime());
      setUnset(updateOperations, AuditPreferenceKeys.createdByUserIds, auditPreference.getCreatedByUserIds());
      setUnset(updateOperations, AuditPreferenceKeys.operationTypes, auditPreference.getOperationTypes());
      setUnset(updateOperations, AuditPreferenceKeys.includeAccountLevelResources,
          auditPreference.isIncludeAccountLevelResources());
      setUnset(
          updateOperations, AuditPreferenceKeys.includeAppLevelResources, auditPreference.isIncludeAppLevelResources());
      setUnset(updateOperations, AuditPreferenceKeys.accountAuditFilter, auditPreference.getAccountAuditFilter());
      setUnset(
          updateOperations, AuditPreferenceKeys.applicationAuditFilter, auditPreference.getApplicationAuditFilter());

      update(accountId, userId, preferenceId, updateOperations);
    }

    // Return updated preference
    return wingsPersistence.get(Preference.class, PreferenceKeys.uuid);
  }

  private void update(
      String accountId, String userId, String preferenceId, UpdateOperations<Preference> updateOperations) {
    wingsPersistence.update(wingsPersistence.createQuery(Preference.class)
                                .filter(PreferenceKeys.accountId, accountId)
                                .filter(PreferenceKeys.userId, userId)
                                .filter(PreferenceKeys.uuid, preferenceId),
        updateOperations);
  }

  @Override
  public void delete(String accountId, String userId, String preferenceId) {
    wingsPersistence.delete(wingsPersistence.createQuery(Preference.class)
                                .filter(PreferenceKeys.accountId, accountId)
                                .filter(USER_ID_KEY, userId)
                                .filter(PreferenceKeys.uuid, preferenceId));
  }
}
