package software.wings.delegatetasks.k8s.watch;

import com.google.inject.ImplementedBy;

import java.util.List;

@ImplementedBy(K8sWatchServiceImpl.class)
public interface K8sWatchService {
  List<String> list();

  String register(WatchRequest watchRequest);

  void remove(String id);
}
