package io.harness.perpetualtask.datacollection.k8s;

import io.harness.cvng.CVNGRequestExecutor;
import io.harness.cvng.beans.K8ActivityDataCollectionInfo;
import io.harness.perpetualtask.k8s.informer.ClusterDetails;
import io.harness.perpetualtask.k8s.informer.handlers.V1DeploymentHandler;
import io.harness.perpetualtask.k8s.informer.handlers.V1ReplicaSetHandler;
import io.harness.perpetualtask.k8s.informer.handlers.V1StatefulSetHandler;
import io.harness.verificationclient.CVNextGenServiceClient;

import com.google.inject.Injector;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentList;
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
    //    addHandlerForDeployment(factory, apiClient, clusterDetails);
    //
    //    addHandlerForStatefulSet(factory, apiClient, clusterDetails);

    return factory;
  }

  private void addHandlerForReplicaSet(SharedInformerFactory factory, ApiClient apiClient, String accountId,
      K8ActivityDataCollectionInfo dataCollectionInfo, Injector injector) {
    AppsV1Api appsV1Api = new AppsV1Api(apiClient);
    ChangeIntelReplicaSetHandler handler = new ChangeIntelReplicaSetHandler(accountId, dataCollectionInfo);
    injector.injectMembers(handler);
    factory
        .sharedIndexInformerFor((CallGeneratorParams params)
                                    -> appsV1Api.listReplicaSetForAllNamespacesCall(null, null, null, null, null, null,
                                        params.resourceVersion, params.timeoutSeconds, params.watch, null),
            V1ReplicaSet.class, V1ReplicaSetList.class)
        .addEventHandler(handler);
  }
  //
  //  private void addHandlerForDeployment(
  //          SharedInformerFactory factory, ApiClient apiClient, K8ActivityDataCollectionInfo dataCollectionInfo,
  //          CVNextGenServiceClient nextGenServiceClient, CVNGRequestExecutor requestExecutor) {
  //    AppsV1Api appsV1Api = new AppsV1Api(apiClient);
  //    factory
  //            .sharedIndexInformerFor((CallGeneratorParams params)
  //                            -> appsV1Api.listDeploymentForAllNamespacesCall(null, null, null, null, null, null,
  //                    params.resourceVersion, params.timeoutSeconds, params.watch, null),
  //                    V1Deployment.class, V1DeploymentList.class)
  //            .addEventHandler(new V1DeploymentHandler(eventPublisher, clusterDetails));
  //  }
  //
  //  private void addHandlerForStatefulSet(
  //          SharedInformerFactory factory, ApiClient apiClient, K8ActivityDataCollectionInfo dataCollectionInfo,
  //          CVNextGenServiceClient nextGenServiceClient, CVNGRequestExecutor requestExecutor) {
  //    AppsV1Api appsV1Api = new AppsV1Api(apiClient);
  //    factory
  //            .sharedIndexInformerFor((CallGeneratorParams params)
  //                            -> appsV1Api.listStatefulSetForAllNamespacesCall(null, null, null, null, null, null,
  //                    params.resourceVersion, params.timeoutSeconds, params.watch, null),
  //                    V1StatefulSet.class, V1StatefulSetList.class)
  //            .addEventHandler(new V1StatefulSetHandler(eventPublisher, clusterDetails));
  //  }
}
