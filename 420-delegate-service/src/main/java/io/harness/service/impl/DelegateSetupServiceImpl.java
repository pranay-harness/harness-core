package io.harness.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.DelegateType.KUBERNETES;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.Delegate.DelegateKeys;
import io.harness.delegate.beans.DelegateConnectionDetails;
import io.harness.delegate.beans.DelegateEntityOwner;
import io.harness.delegate.beans.DelegateGroup;
import io.harness.delegate.beans.DelegateGroupDetails;
import io.harness.delegate.beans.DelegateGroupListing;
import io.harness.delegate.beans.DelegateInsightsDetails;
import io.harness.delegate.beans.DelegateInstanceStatus;
import io.harness.delegate.beans.DelegateProfile;
import io.harness.delegate.utils.DelegateEntityOwnerMapper;
import io.harness.persistence.HPersistence;
import io.harness.service.intfc.DelegateCache;
import io.harness.service.intfc.DelegateInsightsService;
import io.harness.service.intfc.DelegateSetupService;

import software.wings.beans.SelectorType;
import software.wings.service.impl.DelegateConnectionDao;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  @Override
  public DelegateGroupListing listDelegateGroupDetails(String accountId, String orgId, String projectId) {
    List<DelegateGroupDetails> delegateGroupDetails = getDelegateGroupDetails(accountId, orgId, projectId);

    return DelegateGroupListing.builder().delegateGroupDetails(delegateGroupDetails).build();
  }

  private List<DelegateGroupDetails> getDelegateGroupDetails(String accountId, String orgId, String projectId) {
    Map<String, List<DelegateConnectionDetails>> activeDelegateConnections =
        delegateConnectionDao.obtainActiveDelegateConnections(accountId);

    Query<Delegate> delegateQuery = persistence.createQuery(Delegate.class)
                                        .filter(DelegateKeys.accountId, accountId)
                                        .filter(DelegateKeys.ng, true)
                                        .field(DelegateKeys.delegateGroupId)
                                        .exists();

    DelegateEntityOwner owner = DelegateEntityOwnerMapper.buildOwner(orgId, projectId);
    if (owner != null) {
      delegateQuery.filter(DelegateKeys.owner, owner);
    } else {
      // Account level delegates
      delegateQuery.field(DelegateKeys.owner).doesNotExist();
    }
    delegateQuery.field(DelegateKeys.status)
        .hasAnyOf(Arrays.asList(DelegateInstanceStatus.ENABLED, DelegateInstanceStatus.WAITING_FOR_APPROVAL));

    return delegateQuery.asList()
        .stream()
        .collect(groupingBy(Delegate::getDelegateGroupId))
        .entrySet()
        .stream()
        .map(entry -> {
          List<Delegate> groupDelegates = entry.getValue();

          String delegateType = groupDelegates.get(0).getDelegateType();
          String groupName = obtainDelegateGroupName(accountId, entry.getKey(), groupDelegates.get(0));

          String groupHostName = "";
          if (KUBERNETES.equals(delegateType)) {
            groupHostName = getHostNameForGroupedDelegate(groupDelegates.get(0).getHostName());
          }

          Map<String, SelectorType> groupSelectors = new HashMap<>();
          groupDelegates.forEach(delegate -> groupSelectors.putAll(retrieveDelegateImplicitSelectors(delegate)));

          long lastHeartBeat = groupDelegates.stream().mapToLong(Delegate::getLastHeartBeat).max().orElse(0);

          int replicas =
              groupDelegates.get(0).getSizeDetails() != null ? groupDelegates.get(0).getSizeDetails().getReplicas() : 0;

          List<DelegateGroupListing.DelegateInner> delegateInstanceDetails =
              buildInnerDelegates(entry.getValue(), activeDelegateConnections, true);

          return DelegateGroupDetails.builder()
              .delegateType(delegateType)
              .groupName(groupName)
              .groupHostName(groupHostName)
              .groupImplicitSelectors(groupSelectors)
              .delegateInsightsDetails(retrieveDelegateInsightsDetails(accountId, entry.getKey()))
              .lastHeartBeat(lastHeartBeat)
              .activelyConnected(delegateInstanceDetails.stream().anyMatch(
                  delegateDetails -> isNotEmpty(delegateDetails.getConnections())))
              .delegateReplicas(replicas)
              .delegateInstanceDetails(delegateInstanceDetails)
              .build();
        })
        .collect(toList());
  }

  private String obtainDelegateGroupName(String accountId, String delegateGroupId, Delegate delegate) {
    DelegateGroup delegateGroup = delegateCache.getDelegateGroup(accountId, delegateGroupId);
    return delegateGroup != null ? delegateGroup.getName() : delegate.getDelegateName();
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
}
