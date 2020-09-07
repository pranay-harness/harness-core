package io.harness.perpetualtask.k8s.informer.handlers;

import static io.harness.perpetualtask.k8s.informer.handlers.support.WorkloadSpecUtils.makeContainerSpecs;

import com.google.protobuf.Timestamp;

import io.harness.event.client.EventPublisher;
import io.harness.perpetualtask.k8s.informer.ClusterDetails;
import io.harness.perpetualtask.k8s.informer.handlers.support.WorkloadSpecUtils;
import io.harness.perpetualtask.k8s.watch.K8sWorkloadSpec;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1beta1CronJob;

import java.util.List;

public class V1beta1CronJobHandler extends BaseHandler<V1beta1CronJob> {
  public V1beta1CronJobHandler(EventPublisher eventPublisher, ClusterDetails clusterDetails) {
    super(eventPublisher, clusterDetails);
  }

  @Override
  String getKind() {
    return "CronJob";
  }

  @Override
  String getApiVersion() {
    return "batch/v1beta1";
  }

  @Override
  protected void publishWorkloadSpecOnAdd(V1beta1CronJob cronJob, Timestamp occurredAt) {
    if (cronJob.getMetadata() != null && cronJob.getSpec() != null
        && cronJob.getSpec().getJobTemplate().getSpec() != null
        && cronJob.getSpec().getJobTemplate().getSpec().getTemplate().getSpec() != null) {
      List<V1Container> containers =
          cronJob.getSpec().getJobTemplate().getSpec().getTemplate().getSpec().getContainers();
      publishWorkloadSpec(
          K8sWorkloadSpec.newBuilder()
              .setWorkloadKind(getKind())
              .setWorkloadName(cronJob.getMetadata().getName())
              .setNamespace(cronJob.getMetadata().getNamespace())
              .addAllContainerSpecs(WorkloadSpecUtils.makeContainerSpecs(containers))
              .addAllInitContainerSpecs(makeContainerSpecs(
                  cronJob.getSpec().getJobTemplate().getSpec().getTemplate().getSpec().getInitContainers()))
              .build(),
          occurredAt);
    }
  }

  @Override
  protected void publishWorkloadSpecIfChangedOnUpdate(
      V1beta1CronJob oldCronJob, V1beta1CronJob newCronJob, Timestamp occurredAt) {
    if (oldCronJob.getSpec() != null && newCronJob.getSpec() != null
        && oldCronJob.getSpec().getJobTemplate().getSpec() != null
        && newCronJob.getSpec().getJobTemplate().getSpec() != null
        && oldCronJob.getSpec().getJobTemplate().getSpec().getTemplate().getSpec() != null
        && newCronJob.getSpec().getJobTemplate().getSpec().getTemplate().getSpec() != null
        && oldCronJob.getMetadata() != null && newCronJob.getMetadata() != null) {
      List<V1Container> containers =
          oldCronJob.getSpec().getJobTemplate().getSpec().getTemplate().getSpec().getContainers();
      List<V1Container> initContainers =
          oldCronJob.getSpec().getJobTemplate().getSpec().getTemplate().getSpec().getInitContainers();
      K8sWorkloadSpec oldSpecs = K8sWorkloadSpec.newBuilder()
                                     .setWorkloadKind(getKind())
                                     .setWorkloadName(oldCronJob.getMetadata().getName())
                                     .setNamespace(oldCronJob.getMetadata().getNamespace())
                                     .setWorkloadKind(getKind())
                                     .addAllContainerSpecs(makeContainerSpecs(containers))
                                     .addAllInitContainerSpecs(makeContainerSpecs(initContainers))
                                     .build();
      List<V1Container> newContainers =
          newCronJob.getSpec().getJobTemplate().getSpec().getTemplate().getSpec().getContainers();
      List<V1Container> newInitContainers =
          newCronJob.getSpec().getJobTemplate().getSpec().getTemplate().getSpec().getInitContainers();
      K8sWorkloadSpec newSpecs = K8sWorkloadSpec.newBuilder()
                                     .setWorkloadKind(getKind())
                                     .setWorkloadName(newCronJob.getMetadata().getName())
                                     .setNamespace(newCronJob.getMetadata().getNamespace())
                                     .setWorkloadKind(getKind())
                                     .addAllContainerSpecs(makeContainerSpecs(newContainers))
                                     .addAllInitContainerSpecs(makeContainerSpecs(newInitContainers))
                                     .build();
      if (!oldSpecs.equals(newSpecs)) {
        publishWorkloadSpec(newSpecs, occurredAt);
      }
    }
  }
}
