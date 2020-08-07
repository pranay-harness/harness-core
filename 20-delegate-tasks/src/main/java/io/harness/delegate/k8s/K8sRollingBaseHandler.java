package io.harness.delegate.k8s;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;

import io.harness.k8s.model.K8sPod;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class K8sRollingBaseHandler {
  @VisibleForTesting
  public List<K8sPod> tagNewPods(List<K8sPod> newPods, List<K8sPod> existingPods) {
    Set<String> existingPodNames = existingPods.stream().map(K8sPod::getName).collect(Collectors.toSet());
    List<K8sPod> allPods = new ArrayList<>(newPods);
    allPods.forEach(pod -> {
      if (!existingPodNames.contains(pod.getName())) {
        pod.setNewPod(true);
      }
    });
    return allPods;
  }
}
