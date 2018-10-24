package io.harness.k8s.manifest;

import io.harness.k8s.model.KubernetesResource;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class KubernetesResourceComparer implements Comparator<KubernetesResource>, Serializable {
  private static String[] KindOrder = {
      "Namespace",
      "ResourceQuota",
      "LimitRange",
      "PodSecurityPolicy",
      "Secret",
      "ConfigMap",
      "StorageClass",
      "PersistentVolume",
      "PersistentVolumeClaim",
      "ServiceAccount",
      "CustomResourceDefinition",
      "ClusterRole",
      "ClusterRoleBinding",
      "Role",
      "RoleBinding",
      "Service",
      "DaemonSet",
      "Pod",
      "ReplicationController",
      "ReplicaSet",
      "Deployment",
      "StatefulSet",
      "Job",
      "CronJob",
      "Ingress",
      "APIService",
  };

  private static Map<String, Integer> KindOrderMap = new HashMap<>();

  static {
    for (int i = 0; i < KindOrder.length; i++) {
      KindOrderMap.put(KindOrder[i], i);
    }
  }

  @Override
  public int compare(KubernetesResource a, KubernetesResource b) {
    Integer aOrder = KubernetesResourceComparer.KindOrderMap.getOrDefault(
        a.getResourceId().getKind(), Integer.valueOf(KindOrder.length));
    Integer bOrder = KubernetesResourceComparer.KindOrderMap.getOrDefault(
        b.getResourceId().getKind(), Integer.valueOf(KindOrder.length));

    if (aOrder.equals(bOrder)) {
      if (!StringUtils.equals(a.getResourceId().getKind(), b.getResourceId().getKind())) {
        // If kinds are not in known list, and not equal - just sort them in alphabetical order.
        return a.getResourceId().getKind().compareToIgnoreCase(b.getResourceId().getKind());
      }
      // Within a kind - sort by name
      return a.getResourceId().getName().compareToIgnoreCase(b.getResourceId().getName());
    }
    return aOrder.compareTo(bOrder);
  }
}
