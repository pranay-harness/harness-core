package io.harness.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.DelegateType.KUBERNETES;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.Delegate.DelegateKeys;
import io.harness.delegate.beans.DelegateConnectionDetails;
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
import io.harness.delegate.beans.DelegateSizeDetails;
import io.harness.delegate.utils.DelegateEntityOwnerHelper;
import io.harness.ff.FeatureFlagService;
import io.harness.persistence.HPersistence;
import io.harness.service.intfc.DelegateCache;
import io.harness.service.intfc.DelegateInsightsService;
import io.harness.service.intfc.DelegateSetupService;

import software.wings.beans.SelectorType;
import software.wings.service.impl.DelegateConnectionDao;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;

@Singleton
@ValidateOnExecution
@Slf4j
@OwnedBy(HarnessTeam.DEL)
public class DelegateSetupServiceImpl implements DelegateSetupService {
  @Inject private HPersistence persistence;
  @Inject private DelegateCache delegateCache;
  @Inject private DelegateInsightsService delegateInsightsService;
  @Inject private DelegateConnectionDao delegateConnectionDao;
  @Inject private FeatureFlagService featureFlagService;

  @Override
  public DelegateGroupListing listDelegateGroupDetails(String accountId, String orgId, String projectId) {
    List<DelegateGroupDetails> delegateGroupDetails = getDelegateGroupDetails(accountId, orgId, projectId);

    return DelegateGroupListing.builder().delegateGroupDetails(delegateGroupDetails).build();
  }

  @Override
  public DelegateGroupListing listDelegateGroupDetailsUpTheHierarchy(String accountId, String orgId, String projectId) {
    List<DelegateGroupDetails> delegateGroupDetails =
        getDelegateGroupDetailsUpTheHierarchy(accountId, orgId, projectId);

    return DelegateGroupListing.builder().delegateGroupDetails(delegateGroupDetails).build();
  }

  private List<DelegateGroupDetails> getDelegateGroupDetailsUpTheHierarchy(
      String accountId, String orgId, String projectId) {
    Query<DelegateGroup> delegateGroupQuery =
        persistence.createQuery(DelegateGroup.class).filter(DelegateGroupKeys.accountId, accountId);
    delegateGroupQuery = featureFlagService.isEnabled(FeatureName.NG_CG_TASK_ASSIGNMENT_ISOLATION, accountId)
        ? delegateGroupQuery.filter(DelegateGroupKeys.ng, true)
        : delegateGroupQuery.filter(DelegateGroupKeys.ng, false);

    String projectIdentifier = orgId == null || projectId == null ? null : orgId + "/" + projectId;
    delegateGroupQuery.field(DelegateKeys.owner_identifier).in(Arrays.asList(null, orgId, projectIdentifier));

    List<String> delegateGroupIds = delegateGroupQuery.field(DelegateGroupKeys.status)
                                        .notEqual(DelegateGroupStatus.DELETED)
                                        .asKeyList()
                                        .stream()
                                        .map(key -> (String) key.getId())
                                        .collect(toList());

    Map<String, List<Delegate>> delegatesByGroup = persistence.createQuery(Delegate.class)
                                                       .filter(DelegateKeys.accountId, accountId)
                                                       .field(DelegateKeys.delegateGroupId)
                                                       .in(delegateGroupIds)
                                                       .asList()
                                                       .stream()
                                                       .collect(groupingBy(Delegate::getDelegateGroupId));

    return delegateGroupIds.stream()
        .map(delegateGroupId
            -> buildDelegateGroupDetails(accountId, delegateGroupId, delegatesByGroup.get(delegateGroupId)))
        .collect(toList());
  }

  @Override
  public DelegateGroupDetails getDelegateGroupDetails(String accountId, String delegateGroupId) {
    List<Delegate> groupDelegates = persistence.createQuery(Delegate.class)
                                        .filter(DelegateKeys.accountId, accountId)
                                        .filter(DelegateKeys.delegateGroupId, delegateGroupId)
                                        .field(DelegateKeys.status)
                                        .notEqual(DelegateInstanceStatus.DELETED)
                                        .asList();

    return buildDelegateGroupDetails(accountId, delegateGroupId, groupDelegates);
  }

  private List<DelegateGroupDetails> getDelegateGroupDetails(String accountId, String orgId, String projectId) {
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

    List<String> delegateGroupIds = delegateGroupQuery.field(DelegateGroupKeys.status)
                                        .notEqual(DelegateGroupStatus.DELETED)
                                        .asKeyList()
                                        .stream()
                                        .map(key -> (String) key.getId())
                                        .collect(toList());

    Map<String, List<Delegate>> delegatesByGroup = persistence.createQuery(Delegate.class)
                                                       .filter(DelegateKeys.accountId, accountId)
                                                       .field(DelegateKeys.delegateGroupId)
                                                       .in(delegateGroupIds)
                                                       .asList()
                                                       .stream()
                                                       .collect(groupingBy(Delegate::getDelegateGroupId));

    return delegateGroupIds.stream()
        .map(delegateGroupId
            -> buildDelegateGroupDetails(accountId, delegateGroupId, delegatesByGroup.get(delegateGroupId)))
        .collect(toList());
  }

  private DelegateGroupDetails buildDelegateGroupDetails(
      String accountId, String delegateGroupId, List<Delegate> groupDelegates) {
    if (groupDelegates == null) {
      groupDelegates = emptyList();
    }

    Map<String, List<DelegateConnectionDetails>> activeDelegateConnections =
        delegateConnectionDao.obtainActiveDelegateConnections(accountId);

    DelegateGroup delegateGroup = delegateCache.getDelegateGroup(accountId, delegateGroupId);

    String delegateType = delegateGroup != null ? delegateGroup.getDelegateType() : null;
    String groupName = delegateGroup != null ? delegateGroup.getName() : null;
    String delegateDescription = delegateGroup != null ? delegateGroup.getDescription() : null;
    String delegateConfigurationId = delegateGroup != null ? delegateGroup.getDelegateConfigurationId() : null;
    DelegateSizeDetails sizeDetails = delegateGroup != null ? delegateGroup.getSizeDetails() : null;

    String groupHostName = "";
    if (KUBERNETES.equals(delegateType) && isNotEmpty(groupDelegates)) {
      groupHostName = getHostNameForGroupedDelegate(groupDelegates.get(0).getHostName());
    }

    long lastHeartBeat = groupDelegates.stream().mapToLong(Delegate::getLastHeartBeat).max().orElse(0);

    List<DelegateGroupListing.DelegateInner> delegateInstanceDetails =
        buildInnerDelegates(groupDelegates, activeDelegateConnections, true);

    return DelegateGroupDetails.builder()
        .groupId(delegateGroupId)
        .delegateType(delegateType)
        .groupName(groupName)
        .groupHostName(groupHostName)
        .delegateDescription(delegateDescription)
        .delegateConfigurationId(delegateConfigurationId)
        .sizeDetails(sizeDetails)
        .groupImplicitSelectors(retrieveDelegateGroupImplicitSelectors(delegateGroup))
        .delegateInsightsDetails(retrieveDelegateInsightsDetails(accountId, delegateGroupId))
        .lastHeartBeat(lastHeartBeat)
        .activelyConnected(
            delegateInstanceDetails.stream().anyMatch(delegateDetails -> isNotEmpty(delegateDetails.getConnections())))
        .delegateInstanceDetails(delegateInstanceDetails)
        .build();
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

  private List<DelegateGroupListing.DelegateInner> buildInnerDelegates(List<Delegate> delegates,
      Map<String, List<DelegateConnectionDetails>> perDelegateConnections, boolean filterInactiveDelegates) {
    return delegates.stream()
        .filter(delegate -> !filterInactiveDelegates || perDelegateConnections.containsKey(delegate.getUuid()))
        .map(delegate -> {
          List<DelegateConnectionDetails> connections =
              perDelegateConnections.computeIfAbsent(delegate.getUuid(), uuid -> emptyList());
          return DelegateGroupListing.DelegateInner.builder().uuid(delegate.getUuid()).connections(connections).build();
        })
        .collect(Collectors.toList());
  }

  @Override
  public Map<String, SelectorType> retrieveDelegateImplicitSelectors(Delegate delegate) {
    SortedMap<String, SelectorType> selectorTypeMap = new TreeMap<>();

    if (isNotBlank(delegate.getDelegateGroupId())) {
      DelegateGroup delegateGroup =
          delegateCache.getDelegateGroup(delegate.getAccountId(), delegate.getDelegateGroupId());

      if (delegateGroup != null) {
        selectorTypeMap.put(delegateGroup.getName().toLowerCase(), SelectorType.GROUP_NAME);
      }
    } else if (isNotBlank(delegate.getHostName())) {
      selectorTypeMap.put(delegate.getHostName().toLowerCase(), SelectorType.HOST_NAME);
    }

    if (isNotBlank(delegate.getDelegateName())) {
      selectorTypeMap.put(delegate.getDelegateName().toLowerCase(), SelectorType.DELEGATE_NAME);
    }

    DelegateProfile delegateProfile =
        delegateCache.getDelegateProfile(delegate.getAccountId(), delegate.getDelegateProfileId());

    if (delegateProfile != null && isNotBlank(delegateProfile.getName())) {
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

    if (delegateProfile != null && isNotBlank(delegateProfile.getName())) {
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

    Query<DelegateGroup> query = persistence.createQuery(DelegateGroup.class)
                                     .filter(DelegateGroupKeys.accountId, accountId)
                                     .filter(DelegateGroupKeys.ng, true)
                                     .field(DelegateGroupKeys.uuid)
                                     .in(identifiers);

    DelegateEntityOwner owner = DelegateEntityOwnerHelper.buildOwner(orgId, projectId);
    if (owner != null) {
      query.filter(DelegateGroupKeys.owner, owner);
    } else {
      // Account level delegates
      query.field(DelegateGroupKeys.owner).doesNotExist();
    }
    query.field(DelegateGroupKeys.status).notEqual(DelegateGroupStatus.DELETED);

    Set<String> existingRecordsKeys = query.asKeyList().stream().map(key -> (String) key.getId()).collect(toSet());

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
    query.field(DelegateProfileKeys.uuid).in(identifiers);

    List<String> existingRecordsKeys = query.asKeyList().stream().map(key -> (String) key.getId()).collect(toList());

    return identifiers.stream().map(existingRecordsKeys::contains).collect(toList());
  }
}
