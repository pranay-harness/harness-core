package io.harness.perpetualtask.k8s.watch;

import com.google.inject.ImplementedBy;

import io.harness.perpetualtask.k8s.watch.K8SWatch.K8sWatchTaskParams;

import java.util.List;

@ImplementedBy(K8sWatchServiceImpl.class)
public interface K8sWatchService {
  List<String> list();
  String create(K8sWatchTaskParams params);
  void delete(String id);
}
