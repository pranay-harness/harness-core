/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.k8sLabel;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.cluster.dao.K8sWorkloadDao;
import io.harness.ccm.commons.entities.k8s.K8sWorkload;

import software.wings.graphql.datafetcher.billing.BillingStatsDefaultKeys;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataLabelFilter;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class K8sLabelHelper {
  @Inject private K8sWorkloadDao k8sWorkloadDao;

  public Set<String> getWorkloadNamesWithNamespacesFromLabels(
      String accountId, String clusterId, QLBillingDataLabelFilter labelFilter) {
    Map<String, List<String>> labels = new HashMap<>();
    labelFilter.getLabels().forEach(label -> {
      String labelName = label.getName();
      if (labels.containsKey(labelName)) {
        label.getValues().forEach(value -> labels.get(labelName).add(value));
      } else {
        labels.put(labelName, label.getValues());
      }
    });
    List<K8sWorkload> workloads = k8sWorkloadDao.list(accountId, clusterId, labels);
    Set<String> workloadNamesWithNamespaces = new HashSet<>();
    workloads.forEach(workload
        -> workloadNamesWithNamespaces.add(
            workload.getName() + BillingStatsDefaultKeys.TOKEN + workload.getNamespace()));
    return workloadNamesWithNamespaces;
  }

  public Set<String> getWorkloadNamesWithNamespacesFromLabels(
      String accountId, long startTime, long endTime, QLBillingDataLabelFilter labelFilter) {
    Map<String, List<String>> labels = new HashMap<>();
    labelFilter.getLabels().forEach(label -> {
      String labelName = label.getName();
      if (labels.containsKey(labelName)) {
        label.getValues().forEach(value -> labels.get(labelName).add(value));
      } else {
        labels.put(labelName, label.getValues());
      }
    });
    List<K8sWorkload> workloads = k8sWorkloadDao.list(accountId, startTime, endTime, labels);
    Set<String> workloadNamesWithNamespaces = new HashSet<>();
    workloads.forEach(workload
        -> workloadNamesWithNamespaces.add(
            workload.getName() + BillingStatsDefaultKeys.TOKEN + workload.getNamespace()));
    return workloadNamesWithNamespaces;
  }

  public Set<K8sWorkload> getLabelLinks(String accountId, Set<String> workloadNames, String labelName) {
    List<K8sWorkload> workloads = k8sWorkloadDao.list(accountId, workloadNames, labelName);
    Set<K8sWorkload> uniqueWorkloads = new HashSet<>();
    Map<String, Boolean> workloadAlreadyAdded = new HashMap<>();
    workloads.forEach(workload -> {
      String name = workload.getName() + ":" + workload.getNamespace();
      if (!workloadAlreadyAdded.containsKey(name)) {
        workloadAlreadyAdded.put(name, Boolean.TRUE);
        uniqueWorkloads.add(workload);
      }
    });
    return uniqueWorkloads;
  }
}
