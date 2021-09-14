/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.k8s;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.connector.k8Connector.K8sServiceAccountInfoResponse;
import io.harness.delegate.beans.connector.k8Connector.KubernetesConnectionTaskParams;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;

import com.google.inject.Inject;
import io.kubernetes.client.openapi.models.V1TokenReviewStatus;
import io.kubernetes.client.openapi.models.V1UserInfo;
import java.io.IOException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.apache.commons.lang3.NotImplementedException;

@OwnedBy(CE)
public class K8sFetchServiceAccountTask extends AbstractDelegateRunnableTask {
  @Inject private K8sTaskHelperBase k8sTaskHelperBase;

  public K8sFetchServiceAccountTask(DelegateTaskPackage delegateTaskPackage,
      ILogStreamingTaskClient logStreamingTaskClient, Consumer<DelegateTaskResponse> consumer,
      BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) throws IOException {
    KubernetesConnectionTaskParams k8sTaskParams = (KubernetesConnectionTaskParams) parameters;

    final V1TokenReviewStatus v1TokenReviewStatus = k8sTaskHelperBase.fetchTokenReviewStatus(
        k8sTaskParams.getKubernetesClusterConfig(), k8sTaskParams.getEncryptionDetails());

    final V1UserInfo v1UserInfo = v1TokenReviewStatus.getUser();

    return K8sServiceAccountInfoResponse.builder()
        .username(v1UserInfo.getUsername())
        .groups(v1UserInfo.getGroups())
        .build();
  }
}
