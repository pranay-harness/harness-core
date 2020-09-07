package io.harness.perpetualtask.k8s.informer.handlers;

import static io.harness.perpetualtask.k8s.informer.handlers.support.WorkloadSpecUtils.makeContainerSpecs;

import com.google.protobuf.Timestamp;

import io.harness.event.client.EventPublisher;
import io.harness.perpetualtask.k8s.informer.ClusterDetails;
import io.harness.perpetualtask.k8s.watch.K8sWorkloadSpec;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1Job;

import java.util.List;

public class V1JobHandler extends BaseHandler<V1Job> {
  public V1JobHandler(EventPublisher eventPublisher, ClusterDetails clusterDetails) {
    super(eventPublisher, clusterDetails);
  }

  @Override
  String getKind() {
    return "Job";
  }

  @Override
  String getApiVersion() {
    return "batch/v1";
  }

  @Override
  protected void publishWorkloadSpecOnAdd(V1Job job, Timestamp occurredAt) {
    if (job.getMetadata() != null && job.getSpec() != null && job.getSpec().getTemplate().getSpec() != null) {
      List<V1Container> containers = job.getSpec().getTemplate().getSpec().getContainers();
      publishWorkloadSpec(
          K8sWorkloadSpec.newBuilder()
              .setWorkloadKind(getKind())
              .setWorkloadName(job.getMetadata().getName())
              .setNamespace(job.getMetadata().getNamespace())
              .addAllContainerSpecs(makeContainerSpecs(containers))
              .addAllInitContainerSpecs(makeContainerSpecs(job.getSpec().getTemplate().getSpec().getInitContainers()))
              .build(),
          occurredAt);
    }
  }

  @Override
  protected void publishWorkloadSpecIfChangedOnUpdate(V1Job oldJob, V1Job newJob, Timestamp occurredAt) {
    if (oldJob.getSpec() != null && newJob.getSpec() != null && oldJob.getSpec().getTemplate().getSpec() != null
        && newJob.getSpec().getTemplate().getSpec() != null && oldJob.getMetadata() != null
        && newJob.getMetadata() != null) {
      List<V1Container> containers = oldJob.getSpec().getTemplate().getSpec().getContainers();
      List<V1Container> initContainers = oldJob.getSpec().getTemplate().getSpec().getInitContainers();
      K8sWorkloadSpec oldSpecs = K8sWorkloadSpec.newBuilder()
                                     .setWorkloadKind(getKind())
                                     .setWorkloadName(oldJob.getMetadata().getName())
                                     .setNamespace(oldJob.getMetadata().getNamespace())
                                     .setWorkloadKind(getKind())
                                     .addAllContainerSpecs(makeContainerSpecs(containers))
                                     .addAllInitContainerSpecs(makeContainerSpecs(initContainers))
                                     .build();
      List<V1Container> newContainers = newJob.getSpec().getTemplate().getSpec().getContainers();
      List<V1Container> newInitContainers = newJob.getSpec().getTemplate().getSpec().getInitContainers();
      K8sWorkloadSpec newSpecs = K8sWorkloadSpec.newBuilder()
                                     .setWorkloadKind(getKind())
                                     .setWorkloadName(newJob.getMetadata().getName())
                                     .setNamespace(newJob.getMetadata().getNamespace())
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
