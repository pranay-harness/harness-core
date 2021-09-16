package io.harness.perpetualtask.datacollection.k8s;
import io.harness.cvng.CVNGRequestExecutor;
import io.harness.cvng.beans.K8ActivityDataCollectionInfo;
import io.harness.cvng.beans.change.ChangeEventDTO;
import io.harness.cvng.beans.change.ChangeSourceType;
import io.harness.verificationclient.CVNextGenServiceClient;

import com.google.inject.Inject;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.informer.ResourceEventHandler;
import lombok.extern.slf4j.Slf4j;
import org.joor.Reflect;

@Slf4j
public abstract class BaseChangeHandler<ApiType extends KubernetesObject> implements ResourceEventHandler<ApiType> {
  String accountId;
  K8ActivityDataCollectionInfo dataCollectionInfo;
  @Inject CVNGRequestExecutor cvngRequestExecutor;
  @Inject CVNextGenServiceClient cvNextGenServiceClient;
  @Inject K8sHandlerUtils k8sHandlerUtils;

  public BaseChangeHandler(String accountId, K8ActivityDataCollectionInfo dataCollectionInfo) {
    this.accountId = accountId;
    this.dataCollectionInfo = dataCollectionInfo;
  }

  @Override
  public void onAdd(ApiType apiType) {}
  void processAndSendAddEvent(ApiType newResource) {}

  @Override
  public void onUpdate(ApiType oldResource, ApiType newResource) {
    handleMissingKindAndApiVersion(oldResource);
    handleMissingKindAndApiVersion(newResource);
  }
  void processAndSendUpdateEvent(ApiType oldResource, ApiType newResource) {}

  @Override
  public void onDelete(ApiType apiType, boolean b) {}
  void processAndSendDeletedEvent(ApiType newResource) {}

  abstract String getKind();
  abstract String getApiVersion();

  private void handleMissingKindAndApiVersion(ApiType resource) {
    if (Reflect.on(resource).get("kind") == null) {
      Reflect.on(resource).set("kind", getKind());
    }
    if (Reflect.on(resource).get("apiVersion") == null) {
      Reflect.on(resource).set("apiVersion", getApiVersion());
    }
  }

  protected ChangeEventDTO buildChangeEventDTOSkeleton() {
    return ChangeEventDTO.builder()
        .type(ChangeSourceType.KUBERNETES)
        .changeSourceIdentifier(dataCollectionInfo.getChangeSourceIdentifier())
        .serviceIdentifier(dataCollectionInfo.getServiceIdentifier())
        .envIdentifier(dataCollectionInfo.getEnvIdentifier())
        .projectIdentifier(dataCollectionInfo.getProjectIdentifier())
        .orgIdentifier(dataCollectionInfo.getOrgIdentifier())
        .build();
  }

  protected void sendEvent(String accountId, ChangeEventDTO changeEventDTO) {
    Boolean resp =
        cvngRequestExecutor.executeWithRetry(cvNextGenServiceClient.saveChangeEvent(accountId, changeEventDTO))
            .getResource();
    if (resp) {
      log.info("ChangeEvent sent to CVNG for source {}", changeEventDTO.getChangeSourceIdentifier());
    }
  }
}
