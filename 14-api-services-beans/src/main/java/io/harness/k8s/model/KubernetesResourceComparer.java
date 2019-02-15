package io.harness.k8s.model;

import static io.harness.k8s.model.Kind.APIService;
import static io.harness.k8s.model.Kind.ClusterRole;
import static io.harness.k8s.model.Kind.ClusterRoleBinding;
import static io.harness.k8s.model.Kind.ConfigMap;
import static io.harness.k8s.model.Kind.CronJob;
import static io.harness.k8s.model.Kind.CustomResourceDefinition;
import static io.harness.k8s.model.Kind.DaemonSet;
import static io.harness.k8s.model.Kind.Deployment;
import static io.harness.k8s.model.Kind.Ingress;
import static io.harness.k8s.model.Kind.Job;
import static io.harness.k8s.model.Kind.LimitRange;
import static io.harness.k8s.model.Kind.Namespace;
import static io.harness.k8s.model.Kind.PersistentVolume;
import static io.harness.k8s.model.Kind.PersistentVolumeClaim;
import static io.harness.k8s.model.Kind.Pod;
import static io.harness.k8s.model.Kind.PodSecurityPolicy;
import static io.harness.k8s.model.Kind.ReplicaSet;
import static io.harness.k8s.model.Kind.ReplicationController;
import static io.harness.k8s.model.Kind.ResourceQuota;
import static io.harness.k8s.model.Kind.Role;
import static io.harness.k8s.model.Kind.RoleBinding;
import static io.harness.k8s.model.Kind.Secret;
import static io.harness.k8s.model.Kind.Service;
import static io.harness.k8s.model.Kind.ServiceAccount;
import static io.harness.k8s.model.Kind.StatefulSet;
import static io.harness.k8s.model.Kind.StorageClass;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class KubernetesResourceComparer implements Comparator<KubernetesResource>, Serializable {
  private static String[] KindOrder = {
      Namespace.name(),
      ResourceQuota.name(),
      LimitRange.name(),
      PodSecurityPolicy.name(),
      Secret.name(),
      ConfigMap.name(),
      StorageClass.name(),
      PersistentVolume.name(),
      PersistentVolumeClaim.name(),
      ServiceAccount.name(),
      CustomResourceDefinition.name(),
      ClusterRole.name(),
      ClusterRoleBinding.name(),
      Role.name(),
      RoleBinding.name(),
      Service.name(),
      DaemonSet.name(),
      Pod.name(),
      ReplicationController.name(),
      ReplicaSet.name(),
      Deployment.name(),
      StatefulSet.name(),
      Job.name(),
      CronJob.name(),
      Ingress.name(),
      APIService.name(),
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
