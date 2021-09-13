package io.harness.perpetualtask.datacollection.changeintel;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.perpetualtask.k8s.informer.handlers.BaseHandler;
import io.harness.perpetualtask.k8s.informer.handlers.K8sHandlerUtils;
import io.harness.perpetualtask.k8s.informer.handlers.K8sHandlerUtils.ResourceDetails;

import com.google.inject.Inject;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Event;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1ReplicaSet;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.kubernetes.client.openapi.models.V1beta1CronJob;
import io.kubernetes.client.util.Yaml;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joor.Reflect;

@Slf4j
@TargetModule(HarnessModule._420_DELEGATE_AGENT)
public class BaseChangeHandler<ApiType extends KubernetesObject> implements ResourceEventHandler<ApiType> {
  @Inject K8sHandlerUtils k8sHandlerUtils;
  static {
    initModelMap();
  }

  private static void initModelMap() {
    // Workaround for classpath scanning issues with nested jars
    // See https://github.com/kubernetes-client/java/issues/365
    try {
      Reflect.on(Yaml.class).call("initModelMap");
      Map<String, Class<?>> classes = Reflect.on(Yaml.class).get("classes");
      classes.clear();
      classes.put("v1beta1/CronJob", V1beta1CronJob.class);
      classes.put("v1/DaemonSet", V1DaemonSet.class);
      classes.put("v1/Deployment", V1Deployment.class);
      classes.put("v1/Event", V1Event.class);
      classes.put("v1/Job", V1Job.class);
      classes.put("v1/Node", V1Node.class);
      classes.put("v1/Pod", V1Pod.class);
      classes.put("v1/ReplicaSet", V1ReplicaSet.class);
      classes.put("v1/StatefulSet", V1StatefulSet.class);
    } catch (Exception e) {
      log.error("Unexpected exception while loading classes: " + e);
    }
  }

  @Override
  public void onAdd(ApiType apiType) {
    // handleMissingKindAndApiVersion
    log.info("onAdd of new resource");
    log.info("New resource: {}", ResourceDetails.ofResource(apiType));
  }

  @Override
  public void onUpdate(ApiType oldResource, ApiType newResource) {
    if (Reflect.on(oldResource).get("kind") == null) {
      Reflect.on(oldResource).set("kind", "Deployment");
    }
    if (Reflect.on(oldResource).get("apiVersion") == null) {
      Reflect.on(oldResource).set("apiVersion", "apps/v1");
    }

    if (Reflect.on(newResource).get("kind") == null) {
      Reflect.on(newResource).set("kind", "Deployment");
    }
    if (Reflect.on(newResource).get("apiVersion") == null) {
      Reflect.on(newResource).set("apiVersion", "apps/v1");
    }

    ResourceDetails oldResourceDetails = ResourceDetails.ofResource(oldResource);
    ResourceDetails newResourceDetails = ResourceDetails.ofResource(newResource);
    //    log.info("Resource: {} updated from {} to {}", oldResourceDetails, oldResourceDetails.getResourceVersion(),
    //            newResourceDetails.getResourceVersion());
    String oldYaml = k8sHandlerUtils.yamlDump(oldResource);
    String newYaml = k8sHandlerUtils.yamlDump(newResource);
    boolean specChanged = !StringUtils.equals(oldYaml, newYaml);
    if (specChanged) {
      log.info("Spec updated from {} to {}", oldYaml, newYaml);
    }
  }

  @Override
  public void onDelete(ApiType apiType, boolean b) {}
}
