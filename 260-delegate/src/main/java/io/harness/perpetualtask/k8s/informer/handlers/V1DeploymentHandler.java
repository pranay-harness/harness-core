/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.perpetualtask.k8s.informer.handlers;

import static io.harness.perpetualtask.k8s.informer.handlers.support.WorkloadSpecUtils.makeContainerSpecs;

import static com.google.common.base.MoreObjects.firstNonNull;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.event.client.EventPublisher;
import io.harness.perpetualtask.k8s.informer.ClusterDetails;
import io.harness.perpetualtask.k8s.watch.K8sWorkloadSpec;

import com.google.protobuf.Timestamp;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1Deployment;
import java.util.List;

@TargetModule(HarnessModule._420_DELEGATE_AGENT)
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
                              .setUid(deployment.getMetadata().getUid())
                              .addAllContainerSpecs(makeContainerSpecs(containers))
                              .addAllInitContainerSpecs(makeContainerSpecs(initContainers))
                              .setVersion(VERSION)
                              .setReplicas(firstNonNull(deployment.getSpec().getReplicas(), 0))
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
                                     .setReplicas(firstNonNull(oldDeployment.getSpec().getReplicas(), 0))
                                     .setUid(oldDeployment.getMetadata().getUid())
                                     .setVersion(VERSION)
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
                                     .setReplicas(firstNonNull(newDeployment.getSpec().getReplicas(), 0))
                                     .setUid(newDeployment.getMetadata().getUid())
                                     .setVersion(VERSION)
                                     .build();
      if (!oldSpecs.equals(newSpecs)) {
        publishWorkloadSpec(newSpecs, occurredAt);
      }
    }
  }
}
