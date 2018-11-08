package io.harness.k8s.manifest;

import static io.harness.k8s.manifest.ObjectYamlUtils.encodeDot;
import static io.harness.k8s.model.Kind.ConfigMap;
import static io.harness.k8s.model.Kind.DaemonSet;
import static io.harness.k8s.model.Kind.Deployment;
import static io.harness.k8s.model.Kind.Job;
import static io.harness.k8s.model.Kind.Pod;
import static io.harness.k8s.model.Kind.Secret;
import static io.harness.k8s.model.Kind.StatefulSet;

import com.google.common.collect.ImmutableSet;

import io.harness.k8s.model.HarnessAnnotations;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;

public class VersionUtils {
  private static String revisionSeparator = "-";
  private static Set<String> versionedKinds = ImmutableSet.of(ConfigMap.name(), Secret.name());
  private static Set<String> workloadKinds =
      ImmutableSet.of(Deployment.name(), DaemonSet.name(), StatefulSet.name(), Pod.name(), Job.name());

  private static boolean shouldVersion(KubernetesResource resource) {
    if (versionedKinds.contains(resource.getResourceId().getKind())) {
      String isDirectApply =
          (String) resource.getField("metadata.annotations." + encodeDot(HarnessAnnotations.directApply));
      if (StringUtils.equalsIgnoreCase(isDirectApply, "true")) {
        return false;
      }
      return true;
    }
    return false;
  }

  private static boolean shouldUpdateVersionedReferences(KubernetesResource resource) {
    if (workloadKinds.contains(resource.getResourceId().getKind())) {
      return true;
    }
    return false;
  }

  public static void addRevisionNumber(List<KubernetesResource> resources, int revision) {
    Set<KubernetesResourceId> versionedResources = new HashSet<>();
    UnaryOperator<Object> appendRevision = t -> t + revisionSeparator + revision;

    for (KubernetesResource resource : resources) {
      if (shouldVersion(resource)) {
        versionedResources.add(resource.getResourceId().cloneInternal());
        resource.transformName(appendRevision);
      }
    }

    for (KubernetesResource resource : resources) {
      if (shouldUpdateVersionedReferences(resource)) {
        UnaryOperator<Object> updateConfigMapReference = t -> {
          KubernetesResourceId configMapResourceId = KubernetesResourceId.builder()
                                                         .namespace(resource.getResourceId().getNamespace())
                                                         .kind(ConfigMap.name())
                                                         .name((String) t)
                                                         .build();
          if (versionedResources.contains(configMapResourceId)) {
            return t + revisionSeparator + revision;
          }
          return t;
        };

        UnaryOperator<Object> updateSecretReference = t -> {
          KubernetesResourceId secretResourceId = KubernetesResourceId.builder()
                                                      .namespace(resource.getResourceId().getNamespace())
                                                      .kind(Secret.name())
                                                      .name((String) t)
                                                      .build();
          if (versionedResources.contains(secretResourceId)) {
            return t + revisionSeparator + revision;
          }
          return t;
        };

        resource.transformConfigMapAndSecretRef(updateConfigMapReference, updateSecretReference);
      }
    }
  }
}
