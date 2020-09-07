package io.harness.perpetualtask.k8s.informer.handlers;

import static io.harness.perpetualtask.k8s.informer.handlers.support.WorkloadSpecUtils.makeContainerSpecs;

import com.google.protobuf.Timestamp;

import io.harness.event.client.EventPublisher;
import io.harness.perpetualtask.k8s.informer.ClusterDetails;
import io.harness.perpetualtask.k8s.watch.K8sWorkloadSpec;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1Deployment;

import java.util.List;

public class V1DeploymentHandler extends BaseHandler<V1Deployment> {
  public V1DeploymentHandler(EventPublisher eventPublisher, ClusterDetails clusterDetails) {
    super(eventPublisher, clusterDetails);
  }

  @Override
  String getKind() {
    return "Deployment";
  }

  @Override
  String getApiVersion() {
    return "apps/v1";
  }

  @Override
  protected void publishWorkloadSpecOnAdd(V1Deployment deployment, Timestamp occurredAt) {
    if (deployment.getMetadata() != null && deployment.getSpec() != null
        && deployment.getSpec().getTemplate().getSpec() != null) {
      List<V1Container> containers = deployment.getSpec().getTemplate().getSpec().getContainers();
      List<V1Container> initContainers = deployment.getSpec().getTemplate().getSpec().getInitContainers();
      publishWorkloadSpec(K8sWorkloadSpec.newBuilder()
                              .setWorkloadKind(getKind())
                              .setWorkloadName(deployment.getMetadata().getName())
                              .setNamespace(deployment.getMetadata().getNamespace())
                              .addAllContainerSpecs(makeContainerSpecs(containers))
                              .addAllInitContainerSpecs(makeContainerSpecs(initContainers))
                              .build(),
          occurredAt);
    }
  }

  @Override
  protected void publishWorkloadSpecIfChangedOnUpdate(
      V1Deployment oldDeployment, V1Deployment newDeployment, Timestamp occurredAt) {
    if (oldDeployment.getSpec() != null && newDeployment.getSpec() != null
        && oldDeployment.getSpec().getTemplate().getSpec() != null
        && newDeployment.getSpec().getTemplate().getSpec() != null && oldDeployment.getMetadata() != null
        && newDeployment.getMetadata() != null) {
      List<V1Container> containers = oldDeployment.getSpec().getTemplate().getSpec().getContainers();
      List<V1Container> initContainers = oldDeployment.getSpec().getTemplate().getSpec().getInitContainers();
      K8sWorkloadSpec oldSpecs = K8sWorkloadSpec.newBuilder()
                                     .setWorkloadKind(getKind())
                                     .setWorkloadName(oldDeployment.getMetadata().getName())
                                     .setNamespace(oldDeployment.getMetadata().getNamespace())
                                     .setWorkloadKind(getKind())
                                     .addAllContainerSpecs(makeContainerSpecs(containers))
                                     .addAllInitContainerSpecs(makeContainerSpecs(initContainers))
                                     .build();
      List<V1Container> newContainers = newDeployment.getSpec().getTemplate().getSpec().getContainers();
      List<V1Container> newInitContainers = newDeployment.getSpec().getTemplate().getSpec().getInitContainers();
      K8sWorkloadSpec newSpecs = K8sWorkloadSpec.newBuilder()
                                     .setWorkloadKind(getKind())
                                     .setWorkloadName(newDeployment.getMetadata().getName())
                                     .setNamespace(newDeployment.getMetadata().getNamespace())
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
