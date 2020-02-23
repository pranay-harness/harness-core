package software.wings.graphql.datafetcher.k8sLabel;

import com.google.inject.Inject;

import io.harness.ccm.cluster.K8sWorkloadDao;
import io.harness.ccm.cluster.entities.K8sWorkload;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataLabelFilter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class K8sLabelHelper {
  @Inject private K8sWorkloadDao k8sWorkloadDao;

  public Set<String> getWorkloadNamesFromLabels(String accountId, QLBillingDataLabelFilter labelFilter) {
    Map<String, List<String>> labels = new HashMap<>();
    labelFilter.getLabels().forEach(label -> {
      String labelName = label.getName();
      if (labels.containsKey(labelName)) {
        label.getValues().forEach(value -> labels.get(labelName).add(value));
      } else {
        labels.put(labelName, label.getValues());
      }
    });
    List<K8sWorkload> workloads = k8sWorkloadDao.list(accountId, labels);
    Set<String> workloadNames = new HashSet<>();
    workloads.forEach(workload -> workloadNames.add(workload.getName()));
    return workloadNames;
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
