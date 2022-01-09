/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.perpetualtask.k8s.informer.handlers;

import static io.harness.perpetualtask.k8s.informer.handlers.support.WorkloadSpecUtils.makeContainerSpecs;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.event.client.EventPublisher;
import io.harness.perpetualtask.k8s.informer.ClusterDetails;
import io.harness.perpetualtask.k8s.watch.K8sWorkloadSpec;

import com.google.protobuf.Timestamp;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1Job;
import java.util.List;

@TargetModule(HarnessModule._420_DELEGATE_AGENT)
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
              .setUid(job.getMetadata().getUid())
              .setVersion(VERSION)
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
                                     .setUid(oldJob.getMetadata().getUid())
                                     .setWorkloadKind(getKind())
                                     .addAllContainerSpecs(makeContainerSpecs(containers))
                                     .addAllInitContainerSpecs(makeContainerSpecs(initContainers))
                                     .setVersion(VERSION)
                                     .build();
      List<V1Container> newContainers = newJob.getSpec().getTemplate().getSpec().getContainers();
      List<V1Container> newInitContainers = newJob.getSpec().getTemplate().getSpec().getInitContainers();
      K8sWorkloadSpec newSpecs = K8sWorkloadSpec.newBuilder()
                                     .setWorkloadKind(getKind())
                                     .setWorkloadName(newJob.getMetadata().getName())
                                     .setNamespace(newJob.getMetadata().getNamespace())
                                     .setUid(newJob.getMetadata().getUid())
                                     .setWorkloadKind(getKind())
                                     .addAllContainerSpecs(makeContainerSpecs(newContainers))
                                     .addAllInitContainerSpecs(makeContainerSpecs(newInitContainers))
                                     .setVersion(VERSION)
                                     .build();
      if (!oldSpecs.equals(newSpecs)) {
        publishWorkloadSpec(newSpecs, occurredAt);
      }
    }
  }
}
