/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.perpetualtask.datacollection.k8s;

import io.harness.cvng.beans.K8ActivityDataCollectionInfo;

import com.google.inject.Injector;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ConfigMapList;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1ReplicaSet;
import io.kubernetes.client.openapi.models.V1ReplicaSetList;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.kubernetes.client.openapi.models.V1StatefulSetList;
import io.kubernetes.client.util.CallGeneratorParams;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChangeIntelSharedInformerFactory {
  public SharedInformerFactory createInformerFactoryWithHandlers(
      ApiClient apiClient, String accountId, K8ActivityDataCollectionInfo dataCollectionInfo, Injector injector) {
    log.info(
        "Creating new SharedInformerFactory for changeSourceId: {}", dataCollectionInfo.getChangeSourceIdentifier());

    SharedInformerFactory factory = new SharedInformerFactory();
    addHandlerForReplicaSet(factory, apiClient, accountId, dataCollectionInfo, injector);
    addHandlerForDeployment(factory, apiClient, accountId, dataCollectionInfo, injector);
    addHandlerForConfigMap(factory, apiClient, accountId, dataCollectionInfo, injector);
    addHandlerForStatefulSet(factory, apiClient, accountId, dataCollectionInfo, injector);
    addHandlerForPod(factory, apiClient, accountId, dataCollectionInfo, injector);

    return factory;
  }

  private void addHandlerForReplicaSet(SharedInformerFactory factory, ApiClient apiClient, String accountId,
      K8ActivityDataCollectionInfo dataCollectionInfo, Injector injector) {
    AppsV1Api appsV1Api = new AppsV1Api(apiClient);
    ChangeIntelReplicaSetHandler handler =
        ChangeIntelReplicaSetHandler.builder().accountId(accountId).dataCollectionInfo(dataCollectionInfo).build();
    injector.injectMembers(handler);
    factory
        .sharedIndexInformerFor((CallGeneratorParams params)
                                    -> appsV1Api.listReplicaSetForAllNamespacesCall(null, null, null, null, null, null,
                                        params.resourceVersion, params.timeoutSeconds, params.watch, null),
            V1ReplicaSet.class, V1ReplicaSetList.class)
        .addEventHandler(handler);
  }

  private void addHandlerForDeployment(SharedInformerFactory factory, ApiClient apiClient, String accountId,
      K8ActivityDataCollectionInfo dataCollectionInfo, Injector injector) {
    AppsV1Api appsV1Api = new AppsV1Api(apiClient);
    ChangeIntelDeploymentHandler handler =
        ChangeIntelDeploymentHandler.builder().accountId(accountId).dataCollectionInfo(dataCollectionInfo).build();
    injector.injectMembers(handler);
    factory
        .sharedIndexInformerFor((CallGeneratorParams params)
                                    -> appsV1Api.listDeploymentForAllNamespacesCall(null, null, null, null, null, null,
                                        params.resourceVersion, params.timeoutSeconds, params.watch, null),
            V1Deployment.class, V1DeploymentList.class)
        .addEventHandler(handler);
  }

  private void addHandlerForConfigMap(SharedInformerFactory factory, ApiClient apiClient, String accountId,
      K8ActivityDataCollectionInfo dataCollectionInfo, Injector injector) {
    CoreV1Api coreV1Api = new CoreV1Api(apiClient);
    ChangeIntelConfigMapHandler handler =
        ChangeIntelConfigMapHandler.builder().accountId(accountId).dataCollectionInfo(dataCollectionInfo).build();
    injector.injectMembers(handler);
    factory
        .sharedIndexInformerFor((CallGeneratorParams params)
                                    -> coreV1Api.listConfigMapForAllNamespacesCall(null, null, null, null, null, null,
                                        params.resourceVersion, params.timeoutSeconds, params.watch, null),
            V1ConfigMap.class, V1ConfigMapList.class)
        .addEventHandler(handler);
  }

  private void addHandlerForPod(SharedInformerFactory factory, ApiClient apiClient, String accountId,
      K8ActivityDataCollectionInfo dataCollectionInfo, Injector injector) {
    CoreV1Api coreV1Api = new CoreV1Api(apiClient);
    ChangeIntelPodHandler handler =
        ChangeIntelPodHandler.builder().accountId(accountId).dataCollectionInfo(dataCollectionInfo).build();
    injector.injectMembers(handler);
    factory
        .sharedIndexInformerFor((CallGeneratorParams params)
                                    -> coreV1Api.listPodForAllNamespacesCall(null, null, null, null, null, null,
                                        params.resourceVersion, params.timeoutSeconds, params.watch, null),
            V1Pod.class, V1PodList.class)
        .addEventHandler(handler);
  }

  private void addHandlerForStatefulSet(SharedInformerFactory factory, ApiClient apiClient, String accountId,
      K8ActivityDataCollectionInfo dataCollectionInfo, Injector injector) {
    AppsV1Api appsV1Api = new AppsV1Api(apiClient);
    ChangeIntelStatefulSetHandler handler =
        ChangeIntelStatefulSetHandler.builder().accountId(accountId).dataCollectionInfo(dataCollectionInfo).build();
    injector.injectMembers(handler);
    factory
        .sharedIndexInformerFor((CallGeneratorParams params)
                                    -> appsV1Api.listStatefulSetForAllNamespacesCall(null, null, null, null, null, null,
                                        params.resourceVersion, params.timeoutSeconds, params.watch, null),
            V1StatefulSet.class, V1StatefulSetList.class)
        .addEventHandler(handler);
  }
}
