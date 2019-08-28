package io.harness.perpetualtask.k8s.watch;

import com.google.inject.ImplementedBy;

import java.util.List;

@ImplementedBy(K8sWatchServiceImpl.class)
public interface K8sWatchService {
  List<String> list();
  String create(K8sWatchTaskParams params);
  void delete(String id);
}
