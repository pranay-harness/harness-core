package io.harness.perpetualtask.datacollection.changeintel;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.perpetualtask.k8s.informer.handlers.BaseHandler.ResourceDetails;

import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.informer.ResourceEventHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._420_DELEGATE_AGENT)
public class BaseEventHandler<ApiType extends KubernetesObject> implements ResourceEventHandler<ApiType> {
  @Override
  public void onAdd(ApiType apiType) {
    // handleMissingKindAndApiVersion
    log.info("onAdd of new resource");
    log.debug("New resource: {}", ResourceDetails.ofResource(apiType));
  }

  @Override
  public void onUpdate(ApiType apiType, ApiType apiType1) {}

  @Override
  public void onDelete(ApiType apiType, boolean b) {}
}
