/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.queueservice.impl;

import static io.harness.beans.DelegateTask.Status.STARTED;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTask;
import io.harness.beans.DelegateTask.DelegateTaskKeys;
import io.harness.delegate.beans.Delegate;
import io.harness.persistence.HPersistence;
import io.harness.queueservice.infc.DelegateResourceCriteria;
import io.harness.service.intfc.DelegateCache;

import software.wings.beans.TaskType;

import com.google.inject.Inject;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.DEL)
public class OrderByTotalNumberOfTaskAssignedCriteria implements DelegateResourceCriteria {
  @Inject private HPersistence persistence;
  @Inject private DelegateCache delegateCache;
  final Comparator<Map.Entry<String, Integer>> valueComparator = Map.Entry.comparingByValue(Comparator.naturalOrder());

  @Inject
  public OrderByTotalNumberOfTaskAssignedCriteria(HPersistence persistence, DelegateCache delegateCache) {
    this.persistence = persistence;
    this.delegateCache = delegateCache;
  }

  @Override
  public List<Delegate> getFilteredEligibleDelegateList(
      List<Delegate> delegateList, TaskType taskType, String accountId) {
    return listOfDelegatesSortedByNumberOfTaskAssigned(accountId);
  }

  private List<Delegate> listOfDelegatesSortedByNumberOfTaskAssigned(String accountId) {
    TreeMap<String, Integer> numberOfTaskAssigned = new TreeMap<>();
    getTotalNumberOfTaskAssignedInDelegate(accountId).forEach(delegateTask -> {
      if (delegateTask.getDelegateId() != null) {
        numberOfTaskAssigned.put(
            delegateTask.getDelegateId(), numberOfTaskAssigned.getOrDefault(delegateTask.getDelegateId(), 0) + 1);
      }
    });
    return numberOfTaskAssigned.entrySet()
        .stream()
        .sorted(valueComparator)
        .map(entry -> updateDelegateWithNumberTaskAssigned(entry, accountId))
        .collect(Collectors.toList());
  }

  private Delegate updateDelegateWithNumberTaskAssigned(Map.Entry<String, Integer> entry, String accountId) {
    Delegate delegate = getDelegateFromCache(entry.getKey(), accountId);
    if (delegate == null) {
      return null;
    }
    delegate.setNumberOfTaskAssigned(entry.getValue());
    return delegate;
  }

  public Delegate getDelegateFromCache(String delegateId, String accountId) {
    return delegateCache.get(accountId, delegateId, false);
  }

  public List<DelegateTask> getTotalNumberOfTaskAssignedInDelegate(String accountId) {
    return persistence.createQuery(DelegateTask.class)
        .filter(DelegateTaskKeys.accountId, accountId)
        .filter(DelegateTaskKeys.status, STARTED)
        .project(DelegateTaskKeys.delegateId, true)
        .asList();
  }

  private Map<String, Integer> listOfDelegatesSortedByNumberOfTaskAssignedFromRedisCache(String accountId) {
    // TBD
    return null;
  }
}
