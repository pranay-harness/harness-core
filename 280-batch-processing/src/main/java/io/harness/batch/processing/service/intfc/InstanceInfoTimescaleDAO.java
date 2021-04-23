package io.harness.batch.processing.service.intfc;

import io.harness.batch.processing.ccm.InstanceEvent;
import io.harness.batch.processing.ccm.InstanceInfo;
import io.harness.event.payloads.Lifecycle;
import io.harness.perpetualtask.k8s.watch.K8sWorkloadSpec;

import java.util.List;
import lombok.NonNull;

public interface InstanceInfoTimescaleDAO {
  void insertIntoNodeInfo(@NonNull InstanceInfo instanceInfo);

  void insertIntoNodeInfo(@NonNull List<InstanceInfo> instanceInfoList);

  void insertIntoWorkloadInfo(@NonNull String accountId, @NonNull K8sWorkloadSpec workloadSpec);

  void insertIntoPodInfo(@NonNull List<InstanceInfo> instanceInfoList);

  void insertIntoPodInfo(@NonNull InstanceInfo instanceInfo);

  void updatePodStopEvent(@NonNull List<InstanceEvent> instanceEventList);

  void updatePodLifecycleEvent(@NonNull String accountId, @NonNull List<Lifecycle> lifecycleList);

  void updateNodeStopEvent(@NonNull List<InstanceEvent> instanceEventList);

  void updateNodeLifecycleEvent(@NonNull String accountId, @NonNull List<Lifecycle> lifecycleList);
}
