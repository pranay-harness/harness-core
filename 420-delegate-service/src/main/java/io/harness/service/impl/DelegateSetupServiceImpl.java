/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.filter.FilterType.DELEGATEPROFILE;
import static io.harness.mongo.MongoUtils.setUnset;
import static io.harness.service.impl.DelegateConnectivityStatus.GROUP_STATUS_CONNECTED;
import static io.harness.service.impl.DelegateConnectivityStatus.GROUP_STATUS_DISCONNECTED;
import static io.harness.service.impl.DelegateConnectivityStatus.GROUP_STATUS_PARTIALLY_CONNECTED;

import static java.time.Duration.ofMinutes;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.Delegate.DelegateKeys;
import io.harness.delegate.beans.DelegateEntityOwner;
import io.harness.delegate.beans.DelegateGroup;
import io.harness.delegate.beans.DelegateGroup.DelegateGroupKeys;
import io.harness.delegate.beans.DelegateGroupDetails;
import io.harness.delegate.beans.DelegateGroupListing;
import io.harness.delegate.beans.DelegateGroupStatus;
import io.harness.delegate.beans.DelegateInsightsDetails;
import io.harness.delegate.beans.DelegateInstanceStatus;
import io.harness.delegate.beans.DelegateProfile;
import io.harness.delegate.beans.DelegateProfile.DelegateProfileKeys;
import io.harness.delegate.filter.DelegateFilterPropertiesDTO;
import io.harness.delegate.utils.DelegateEntityOwnerHelper;
import io.harness.exception.InvalidRequestException;
import io.harness.filter.dto.FilterDTO;
import io.harness.filter.service.FilterService;
import io.harness.persistence.HPersistence;
import io.harness.service.intfc.DelegateCache;
import io.harness.service.intfc.DelegateInsightsService;
import io.harness.service.intfc.DelegateSetupService;

import software.wings.beans.SelectorType;
import software.wings.service.impl.DelegateConnectionDao;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.validation.executable.ValidateOnExecution;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Singleton
@ValidateOnExecution
@Slf4j
@OwnedBy(HarnessTeam.DEL)
public class DelegateSetupServiceImpl implements DelegateSetupService {
  @Inject private HPersistence persistence;
  @Inject private DelegateCache delegateCache;
  @Inject private DelegateInsightsService delegateInsightsService;
  @Inject private DelegateConnectionDao delegateConnectionDao;
  @Inject private FilterService filterService;
  private static final Duration HEARTBEAT_EXPIRY_TIME = ofMinutes(5);

  @Override
  public long getDelegateGroupCount(
      final String accountId, @Nullable final String orgId, @Nullable final String projectId) {
    return createDelegateGroupsQuery(accountId, orgId, projectId, false).count();
  }

  @Override
  public DelegateGroupListing listDelegateGroupDetails(String accountId, String orgId, String projectId) {
    final List<DelegateGroupDetails> delegateGroupDetails =
        getDelegateGroupDetails(accountId, orgId, projectId, false, null);

    return DelegateGroupListing.builder().delegateGroupDetails(delegateGroupDetails).build();
  }

  @Override
  public DelegateGroupListing listDelegateGroupDetailsUpTheHierarchy(String accountId, String orgId, String projectId) {
    final List<DelegateGroupDetails> delegateGroupDetails =
        getDelegateGroupDetails(accountId, orgId, projectId, true, null);

    return DelegateGroupListing.builder().delegateGroupDetails(delegateGroupDetails).build();
  }

  @Override
  public DelegateGroupDetails getDelegateGroupDetails(String accountId, String delegateGroupId) {
    List<Delegate> groupDelegates = persistence.createQuery(Delegate.class)
                                        .filter(DelegateKeys.accountId, accountId)
                                        .filter(DelegateKeys.delegateGroupId, delegateGroupId)
                                        .field(DelegateKeys.status)
                                        .notEqual(DelegateInstanceStatus.DELETED)
                                        .asList();
    DelegateGroup delegateGroup = delegateCache.getDelegateGroup(accountId, delegateGroupId);

    return buildDelegateGroupDetails(accountId, delegateGroup, groupDelegates, delegateGroupId);
  }

  @Override
  public DelegateGroupDetails getDelegateGroupDetailsV2(
      String accountId, String orgId, String projectId, String identifier) {
    DelegateEntityOwner owner = DelegateEntityOwnerHelper.buildOwner(orgId, projectId);

    DelegateGroup delegateGroup =
        delegateCache.getDelegateGroupByAccountAndOwnerAndIdentifier(accountId, owner, identifier);

    String delegateGroupId = delegateGroup != null ? delegateGroup.getUuid() : null;
    List<Delegate> groupDelegates = persistence.createQuery(Delegate.class)
                                        .filter(DelegateKeys.accountId, accountId)
                                        .filter(DelegateKeys.delegateGroupId, delegateGroupId)
                                        .field(DelegateKeys.status)
                                        .notEqual(DelegateInstanceStatus.DELETED)
                                        .asList();

    return buildDelegateGroupDetails(accountId, delegateGroup, groupDelegates, delegateGroupId);
  }

  @Override
  public String getHostNameForGroupedDelegate(String hostname) {
    if (isNotEmpty(hostname)) {
      int indexOfLastHyphen = hostname.lastIndexOf('-');
      if (indexOfLastHyphen > 0) {
        hostname = hostname.substring(0, indexOfLastHyphen) + "-{n}";
      }
    }

    return hostname;
  }

  private DelegateInsightsDetails retrieveDelegateInsightsDetails(String accountId, String delegateGroupId) {
    return delegateInsightsService.retrieveDelegateInsightsDetails(
        accountId, delegateGroupId, System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1));
  }

  @Override
  public Map<String, SelectorType> retrieveDelegateImplicitSelectors(Delegate delegate) {
    SortedMap<String, SelectorType> selectorTypeMap = new TreeMap<>();

    if (isNotEmpty(delegate.getDelegateGroupId())) {
      DelegateGroup delegateGroup =
          delegateCache.getDelegateGroup(delegate.getAccountId(), delegate.getDelegateGroupId());

      if (delegateGroup != null) {
        selectorTypeMap.put(delegateGroup.getName().toLowerCase(), SelectorType.GROUP_NAME);

        if (isNotEmpty(delegateGroup.getTags())) {
          for (String selector : delegateGroup.getTags()) {
            selectorTypeMap.put(selector.toLowerCase(), SelectorType.GROUP_SELECTORS);
          }
        }
      }
    } else if (isNotEmpty(delegate.getHostName())) {
      selectorTypeMap.put(delegate.getHostName().toLowerCase(), SelectorType.HOST_NAME);
    }

    if (isNotEmpty(delegate.getDelegateName())) {
      selectorTypeMap.put(delegate.getDelegateName().toLowerCase(), SelectorType.DELEGATE_NAME);
    }

    DelegateProfile delegateProfile =
        delegateCache.getDelegateProfile(delegate.getAccountId(), delegate.getDelegateProfileId());

    if (delegateProfile != null && isNotEmpty(delegateProfile.getName())) {
      selectorTypeMap.put(delegateProfile.getName().toLowerCase(), SelectorType.PROFILE_NAME);
    }

    if (delegateProfile != null && isNotEmpty(delegateProfile.getSelectors())) {
      for (String selector : delegateProfile.getSelectors()) {
        selectorTypeMap.put(selector.toLowerCase(), SelectorType.PROFILE_SELECTORS);
      }
    }

    return selectorTypeMap;
  }

  @Override
  public Map<String, SelectorType> retrieveDelegateGroupImplicitSelectors(final DelegateGroup delegateGroup) {
    final SortedMap<String, SelectorType> result = new TreeMap<>();

    if (delegateGroup == null) {
      return result;
    }

    result.put(delegateGroup.getName().toLowerCase(), SelectorType.GROUP_NAME);

    final DelegateProfile delegateProfile =
        delegateCache.getDelegateProfile(delegateGroup.getAccountId(), delegateGroup.getDelegateConfigurationId());

    if (delegateProfile != null && isNotEmpty(delegateProfile.getName())) {
      result.put(delegateProfile.getName().toLowerCase(), SelectorType.PROFILE_NAME);
    }

    if (delegateProfile != null && isNotEmpty(delegateProfile.getSelectors())) {
      for (final String selector : delegateProfile.getSelectors()) {
        result.put(selector.toLowerCase(), SelectorType.PROFILE_SELECTORS);
      }
    }
    return result;
  }

  @Override
  public List<Boolean> validateDelegateGroups(
      String accountId, String orgId, String projectId, List<String> identifiers) {
    if (isEmpty(identifiers)) {
      return emptyList();
    }
    DelegateEntityOwner owner = DelegateEntityOwnerHelper.buildOwner(orgId, projectId);

    Query<DelegateGroup> queryByUuids = persistence.createQuery(DelegateGroup.class)
                                            .filter(DelegateGroupKeys.accountId, accountId)
                                            .filter(DelegateGroupKeys.ng, true)
                                            .field(DelegateGroupKeys.uuid)
                                            .in(identifiers);

    Query<DelegateGroup> queryByIdentifiers = persistence.createQuery(DelegateGroup.class)
                                                  .filter(DelegateGroupKeys.accountId, accountId)
                                                  .filter(DelegateGroupKeys.ng, true)
                                                  .filter(DelegateGroupKeys.owner, owner)
                                                  .field(DelegateGroupKeys.identifier)
                                                  .in(identifiers)
                                                  .field(DelegateGroupKeys.status)
                                                  .notEqual(DelegateGroupStatus.DELETED);

    if (owner != null) {
      queryByUuids.filter(DelegateGroupKeys.owner, owner);
    } else {
      // Account level delegates
      queryByUuids.field(DelegateGroupKeys.owner).doesNotExist();
    }
    queryByUuids.field(DelegateGroupKeys.status).notEqual(DelegateGroupStatus.DELETED);

    // We need to fetch all entries by both uuid and identifiers, because it is possible to have them in the list
    // Identifiers are part of NG as user friendly ids which are used as unique identifier in combination with accountId
    // and owner.
    Set<String> existingRecordsKeys =
        queryByUuids.asKeyList().stream().map(key -> (String) key.getId()).collect(toSet());

    Set<String> existingRecordsIdentifiers =
        queryByIdentifiers.asList().stream().map(DelegateGroup::getIdentifier).collect(toSet());

    // creating union from two sets
    existingRecordsKeys.addAll(existingRecordsIdentifiers);

    return identifiers.stream().map(existingRecordsKeys::contains).collect(toList());
  }

  @Override
  public List<Boolean> validateDelegateConfigurations(
      String accountId, String orgId, String projectId, List<String> identifiers) {
    if (isEmpty(identifiers)) {
      return emptyList();
    }

    Query<DelegateProfile> query = persistence.createQuery(DelegateProfile.class)
                                       .filter(DelegateProfileKeys.accountId, accountId)
                                       .filter(DelegateProfileKeys.ng, true);

    DelegateEntityOwner owner = DelegateEntityOwnerHelper.buildOwner(orgId, projectId);
    if (owner != null) {
      query.field(DelegateProfileKeys.owner).equal(owner);
    } else {
      // Account level delegate configurations
      query.field(DelegateProfileKeys.owner).doesNotExist();
    }
    Query<DelegateProfile> filterIdentifiersQuery = query.cloneQuery();
    query.field(DelegateProfileKeys.uuid).in(identifiers);
    List<String> existingRecordsKeys = query.asKeyList().stream().map(key -> (String) key.getId()).collect(toList());

    filterIdentifiersQuery.field(DelegateProfileKeys.identifier).in(identifiers);
    List<String> existingRecordsIdentifiers =
        filterIdentifiersQuery.asList().stream().map(DelegateProfile::getIdentifier).collect(toList());
    return identifiers.stream()
        .map(i -> existingRecordsKeys.contains(i) || existingRecordsIdentifiers.contains(i))
        .collect(toList());
  }

  @Override
  public DelegateGroupDetails updateDelegateGroup(
      String accountId, String delegateGroupId, DelegateGroupDetails delegateGroupDetails) {
    Query<DelegateGroup> updateQuery = persistence.createQuery(DelegateGroup.class)
                                           .filter(DelegateKeys.accountId, accountId)
                                           .filter(DelegateKeys.uuid, delegateGroupId);

    UpdateOperations<DelegateGroup> updateOperations = persistence.createUpdateOperations(DelegateGroup.class);
    setUnset(updateOperations, DelegateGroupKeys.tags, delegateGroupDetails.getGroupCustomSelectors());

    DelegateGroup updatedDelegateGroup =
        persistence.findAndModify(updateQuery, updateOperations, HPersistence.returnNewOptions);
    delegateCache.invalidateDelegateGroupCache(accountId, delegateGroupId);

    return buildDelegateGroupDetails(accountId, updatedDelegateGroup, null, delegateGroupId);
  }

  @Override
  public DelegateGroupDetails updateDelegateGroup(
      String accountId, String orgId, String projectId, String identifier, DelegateGroupDetails delegateGroupDetails) {
    DelegateEntityOwner owner = DelegateEntityOwnerHelper.buildOwner(orgId, projectId);

    Query<DelegateGroup> updateQuery = persistence.createQuery(DelegateGroup.class)
                                           .filter(DelegateGroupKeys.accountId, accountId)
                                           .filter(DelegateGroupKeys.owner, owner)
                                           .filter(DelegateGroupKeys.identifier, identifier);

    UpdateOperations<DelegateGroup> updateOperations = persistence.createUpdateOperations(DelegateGroup.class);
    setUnset(updateOperations, DelegateGroupKeys.tags, delegateGroupDetails.getGroupCustomSelectors());

    DelegateGroup updatedDelegateGroup =
        persistence.findAndModify(updateQuery, updateOperations, HPersistence.returnNewOptions);
    delegateCache.invalidateDelegateGroupCacheByIdentifier(accountId, owner, identifier);

    String delegateGroupId = updatedDelegateGroup != null ? updatedDelegateGroup.getUuid() : null;
    return buildDelegateGroupDetails(accountId, updatedDelegateGroup, null, delegateGroupId);
  }

  private List<DelegateGroupDetails> getDelegateGroupDetails(final String accountId, final String orgId,
      final String projectId, final boolean upTheHierarchy, String delegateTokenName) {
    final Query<DelegateGroup> query = createDelegateGroupsQuery(accountId, orgId, projectId, upTheHierarchy);

    final List<String> delegateGroupIds = query.asKeyList().stream().map(key -> (String) key.getId()).collect(toList());

    Query<Delegate> delegateQuery = persistence.createQuery(Delegate.class)
                                        .filter(DelegateKeys.accountId, accountId)
                                        .field(DelegateKeys.delegateGroupId)
                                        .in(delegateGroupIds);

    if (isNotEmpty(delegateTokenName)) {
      delegateQuery = delegateQuery.filter(DelegateKeys.delegateTokenName, delegateTokenName);
    }

    final Map<String, List<Delegate>> delegatesByGroup =
        delegateQuery.asList().stream().collect(groupingBy(Delegate::getDelegateGroupId));

    return delegateGroupIds.stream()
        .filter(delegateGroupId
            -> isEmpty(delegateTokenName)
                || (delegatesByGroup.get(delegateGroupId) != null && !delegatesByGroup.get(delegateGroupId).isEmpty()))
        .map(delegateGroupId -> {
          DelegateGroup delegateGroup = delegateCache.getDelegateGroup(accountId, delegateGroupId);
          return buildDelegateGroupDetails(
              accountId, delegateGroup, delegatesByGroup.get(delegateGroupId), delegateGroupId);
        })
        .collect(toList());
  }

  private Query<DelegateGroup> createDelegateGroupsQuery(final String accountId, @Nullable final String orgId,
      @Nullable final String projectId, final boolean upTheHierarchy) {
    final Query<DelegateGroup> delegateGroupQuery = persistence.createQuery(DelegateGroup.class)
                                                        .filter(DelegateGroupKeys.accountId, accountId)
                                                        .filter(DelegateGroupKeys.ng, true);

    final DelegateEntityOwner owner = DelegateEntityOwnerHelper.buildOwner(orgId, projectId);
    if (owner != null) {
      if (upTheHierarchy) {
        final String projectIdentifier = projectId != null ? owner.getIdentifier() : null;
        delegateGroupQuery.field(DelegateKeys.owner_identifier).in(Arrays.asList(null, orgId, projectIdentifier));
      } else {
        delegateGroupQuery.filter(DelegateGroupKeys.owner, owner);
      }
    } else {
      // Account level delegates
      delegateGroupQuery.field(DelegateGroupKeys.owner).doesNotExist();
    }

    return delegateGroupQuery.field(DelegateGroupKeys.status).notEqual(DelegateGroupStatus.DELETED);
  }

  private DelegateGroupDetails buildDelegateGroupDetails(
      String accountId, DelegateGroup delegateGroup, List<Delegate> groupDelegates, String delegateGroupId) {
    if (groupDelegates == null) {
      log.info("There are no delegates related to this delegate group.");
      groupDelegates = emptyList();
    }

    String delegateType = delegateGroup != null ? delegateGroup.getDelegateType() : null;
    String groupName = delegateGroup != null ? delegateGroup.getName() : null;
    String delegateDescription = delegateGroup != null ? delegateGroup.getDescription() : null;
    String delegateConfigurationId = delegateGroup != null ? delegateGroup.getDelegateConfigurationId() : null;
    String delegateGroupIdentifier = delegateGroup != null ? delegateGroup.getIdentifier() : null;
    Set<String> groupCustomSelectors = delegateGroup != null ? delegateGroup.getTags() : null;

    long lastHeartBeat = groupDelegates.stream().mapToLong(Delegate::getLastHeartBeat).max().orElse(0);
    AtomicInteger countOfDelegatesConnected = new AtomicInteger();
    List<DelegateGroupListing.DelegateInner> delegateInstanceDetails =
        groupDelegates.stream()
            .map(delegate -> {
              countOfDelegatesConnected.addAndGet(
                  (delegate.getLastHeartBeat() > System.currentTimeMillis() - HEARTBEAT_EXPIRY_TIME.toMillis()) ? 1
                                                                                                                : 0);
              return DelegateGroupListing.DelegateInner.builder()
                  .uuid(delegate.getUuid())
                  .lastHeartbeat(delegate.getLastHeartBeat())
                  .activelyConnected(
                      delegate.getLastHeartBeat() > System.currentTimeMillis() - HEARTBEAT_EXPIRY_TIME.toMillis())
                  .hostName(delegate.getHostName())
                  .build();
            })
            .collect(Collectors.toList());
    String connectivityStatus = GROUP_STATUS_PARTIALLY_CONNECTED;
    if (countOfDelegatesConnected.get() == 0) {
      connectivityStatus = GROUP_STATUS_DISCONNECTED;
    } else if (countOfDelegatesConnected.get() >= groupDelegates.size()) {
      connectivityStatus = GROUP_STATUS_CONNECTED;
    }

    return DelegateGroupDetails.builder()
        .groupId(delegateGroupId)
        .delegateGroupIdentifier(delegateGroupIdentifier)
        .delegateType(delegateType)
        .groupName(groupName)
        .delegateDescription(delegateDescription)
        .delegateConfigurationId(delegateConfigurationId)
        .groupImplicitSelectors(retrieveDelegateGroupImplicitSelectors(delegateGroup))
        .groupCustomSelectors(groupCustomSelectors)
        .delegateInsightsDetails(retrieveDelegateInsightsDetails(accountId, delegateGroupId))
        .lastHeartBeat(lastHeartBeat)
        .delegateInstanceDetails(delegateInstanceDetails)
        .connectivityStatus(connectivityStatus)
        .activelyConnected(!connectivityStatus.equals(GROUP_STATUS_DISCONNECTED))
        .build();
  }

  @Override
  public DelegateGroupListing listDelegateGroupDetailsV2(String accountId, String orgId, String projectId,
      String filterIdentifier, String searchTerm, DelegateFilterPropertiesDTO filterProperties) {
    if (isNotEmpty(filterIdentifier) && filterProperties != null) {
      throw new InvalidRequestException("Can not apply both filter properties and saved filter together");
    }

    if (isNotEmpty(filterIdentifier)) {
      FilterDTO filterDTO = filterService.get(accountId, orgId, projectId, filterIdentifier, DELEGATEPROFILE);
      filterProperties = (DelegateFilterPropertiesDTO) filterDTO.getFilterProperties();
    }

    List<String> delegateGroupIds = getDelegateGroupIds(accountId, orgId, projectId, filterProperties, searchTerm);

    final Map<String, List<Delegate>> delegatesByGroup = persistence.createQuery(Delegate.class)
                                                             .filter(DelegateKeys.accountId, accountId)
                                                             .field(DelegateKeys.delegateGroupId)
                                                             .in(delegateGroupIds)
                                                             .asList()
                                                             .stream()
                                                             .collect(groupingBy(Delegate::getDelegateGroupId));

    List<DelegateGroupDetails> delegateGroupDetails =
        delegateGroupIds.stream()
            .map(delegateGroupId -> {
              DelegateGroup delegateGroup = delegateCache.getDelegateGroup(accountId, delegateGroupId);
              return buildDelegateGroupDetails(
                  accountId, delegateGroup, delegatesByGroup.get(delegateGroupId), delegateGroupId);
            })
            .sorted(new DelegateGroupDetailsComparator())
            .collect(toList());
    return DelegateGroupListing.builder().delegateGroupDetails(delegateGroupDetails).build();
  }

  @Override
  public DelegateGroupListing listDelegateGroupDetails(
      String accountId, String orgId, String projectId, String delegateTokenName) {
    return DelegateGroupListing.builder()
        .delegateGroupDetails(getDelegateGroupDetails(accountId, orgId, projectId, false, delegateTokenName))
        .build();
  }

  private List<String> getDelegateGroupIds(String accountId, String orgId, String projectId,
      DelegateFilterPropertiesDTO filterProperties, String searchTerm) {
    Query<DelegateGroup> delegateGroupQuery = persistence.createQuery(DelegateGroup.class)
                                                  .filter(DelegateGroupKeys.accountId, accountId)
                                                  .filter(DelegateGroupKeys.ng, true);
    DelegateEntityOwner owner = DelegateEntityOwnerHelper.buildOwner(orgId, projectId);
    if (owner != null) {
      delegateGroupQuery.filter(DelegateGroupKeys.owner, owner);
    } else {
      // Account level delegates
      delegateGroupQuery.field(DelegateGroupKeys.owner).doesNotExist();
    }

    if (isNotEmpty(searchTerm)) {
      Criteria criteria =
          delegateGroupQuery.or(delegateGroupQuery.criteria(DelegateGroupKeys.name).contains(searchTerm),
              delegateGroupQuery.criteria(DelegateGroupKeys.description).contains(searchTerm),
              delegateGroupQuery.criteria(DelegateGroupKeys.identifier).contains(searchTerm),
              delegateGroupQuery.criteria(DelegateGroupKeys.tags).hasThisOne(searchTerm));
      delegateGroupQuery.and(criteria);
    }

    if (filterProperties != null) {
      if (isNotEmpty(filterProperties.getDelegateGroupIdentifier())) {
        delegateGroupQuery.field(DelegateGroupKeys.identifier).contains(filterProperties.getDelegateGroupIdentifier());
      }

      if (isNotEmpty(filterProperties.getDelegateName())) {
        delegateGroupQuery.field(DelegateGroupKeys.name).contains(filterProperties.getDelegateName());
      }

      if (isNotEmpty(filterProperties.getDelegateType())) {
        delegateGroupQuery.field(DelegateGroupKeys.delegateType).contains(filterProperties.getDelegateType());
      }

      if (isNotEmpty(filterProperties.getDescription())) {
        delegateGroupQuery.field(DelegateGroupKeys.description).contains(filterProperties.getDescription());
      }
    }
    return delegateGroupQuery.field(DelegateGroupKeys.status)
        .notEqual(DelegateGroupStatus.DELETED)
        .asKeyList()
        .stream()
        .map(key -> (String) key.getId())
        .collect(toList());
  }
}
