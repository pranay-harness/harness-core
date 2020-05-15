package io.harness.perpetualtask.k8s.informer.handlers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Timestamp;

import io.harness.ccm.health.HealthStatusService;
import io.harness.event.client.EventPublisher;
import io.harness.grpc.utils.HTimestamps;
import io.harness.perpetualtask.k8s.informer.ClusterDetails;
import io.harness.perpetualtask.k8s.watch.K8sObjectReference;
import io.harness.perpetualtask.k8s.watch.K8sWatchEvent;
import io.harness.reflection.ReflectionUtils;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Event;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1ObjectMetaBuilder;
import io.kubernetes.client.openapi.models.V1OwnerReference;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1ReplicaSet;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.kubernetes.client.openapi.models.V1beta1CronJob;
import io.kubernetes.client.util.Yaml;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joor.Reflect;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public abstract class BaseHandler<ApiType> implements ResourceEventHandler<ApiType> {
  private static final String METADATA = "metadata";

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
      logger.error("Unexpected exception while loading classes: " + e);
    }
  }

  private final EventPublisher eventPublisher;
  private final K8sWatchEvent clusterDetailsProto;

  public BaseHandler(EventPublisher eventPublisher, ClusterDetails clusterDetails) {
    this.eventPublisher = eventPublisher;
    this.clusterDetailsProto = K8sWatchEvent.newBuilder()
                                   .setClusterId(clusterDetails.getClusterId())
                                   .setCloudProviderId(clusterDetails.getCloudProviderId())
                                   .setClusterName(clusterDetails.getClusterName())
                                   .setKubeSystemUid(clusterDetails.getKubeSystemUid())
                                   .build();
  }

  protected void publishMessage(K8sWatchEvent resourceDetailsProto, Timestamp occurredAt) {
    eventPublisher.publishMessage(K8sWatchEvent.newBuilder(clusterDetailsProto).mergeFrom(resourceDetailsProto).build(),
        occurredAt, ImmutableMap.of(HealthStatusService.CLUSTER_ID_IDENTIFIER, clusterDetailsProto.getClusterId()));
  }

  @Override
  public void onAdd(ApiType resource) {
    handleMissingKindAndApiVersion(resource);
    logger.debug("Added resource: {}", ResourceDetails.ofResource(resource));
    V1OwnerReference controller = getController(resource);
    if (controller != null) {
      logger.debug("Skipping publish for resource added as it has controller: {}", controller);
    } else {
      V1ObjectMeta objectMeta = getMetadata(resource);
      Instant creationTimestamp = Optional.ofNullable(objectMeta)
                                      .map(V1ObjectMeta::getCreationTimestamp)
                                      .map(dt -> Instant.ofEpochMilli(dt.getMillis()))
                                      .orElse(Instant.EPOCH);
      publishMessage(K8sWatchEvent.newBuilder()
                         .setType(K8sWatchEvent.Type.TYPE_ADDED)
                         .setResourceRef(createObjectReference(resource))
                         .setNewResourceVersion(getResourceVersion(resource))
                         .setNewResourceYaml(yamlDump(resource))
                         .setDescription(String.format("%s created", getKind()))
                         .build(),
          HTimestamps.fromInstant(creationTimestamp));
    }
  }

  private void handleMissingKindAndApiVersion(ApiType resource) {
    if (Reflect.on(resource).get("kind") == null) {
      Reflect.on(resource).set("kind", getKind());
    }
    if (Reflect.on(resource).get("apiVersion") == null) {
      Reflect.on(resource).set("apiVersion", getApiVersion());
    }
  }

  @Override
  public void onUpdate(ApiType oldResource, ApiType newResource) {
    handleMissingKindAndApiVersion(oldResource);
    handleMissingKindAndApiVersion(newResource);
    ResourceDetails oldResourceDetails = ResourceDetails.ofResource(oldResource);
    ResourceDetails newResourceDetails = ResourceDetails.ofResource(newResource);
    logger.debug("Resource: {} updated from {} to {}", oldResourceDetails, oldResourceDetails.getResourceVersion(),
        newResourceDetails.getResourceVersion());
    String oldYaml = yamlDump(oldResource);
    String newYaml = yamlDump(newResource);
    // Publish change event only if the spec changes, and the resource does not have a controlling owner (in which case
    // the change will be captured in owner)
    boolean specChanged = !StringUtils.equals(oldYaml, newYaml);
    V1OwnerReference controller = getController(oldResource);
    K8sObjectReference objectReference = createObjectReference(oldResource);
    logger.debug("Updated Resource: {}, SpecChanged: {}, controller:{}", objectReference, specChanged, controller);
    if (controller != null) {
      logger.debug("Skipping publish for resource updated as it has controller: {}", controller);
    } else if (!specChanged) {
      logger.debug("Skipping publish for resource updated since no yaml change");
    } else {
      publishMessage(K8sWatchEvent.newBuilder()
                         .setType(K8sWatchEvent.Type.TYPE_UPDATED)
                         .setResourceRef(objectReference)
                         .setOldResourceVersion(getResourceVersion(oldResource))
                         .setOldResourceYaml(oldYaml)
                         .setNewResourceVersion(getResourceVersion(newResource))
                         .setNewResourceYaml(newYaml)
                         .setDescription(String.format("%s updated", getKind()))
                         .build(),
          HTimestamps.fromInstant(Instant.now()));
    }
  }

  private ApiType clone(ApiType resource) {
    try {
      @SuppressWarnings("unchecked") ApiType copy = (ApiType) Yaml.load(Yaml.dump(resource));
      return copy;
    } catch (IOException e) {
      logger.warn("Serialization round trip should clone", e);
      return resource;
    }
  }

  private String yamlDump(ApiType resource) {
    // to avoid mutating resource
    ApiType copy = clone(resource);
    Reflect.on(copy).set("status", null);
    V1ObjectMetaBuilder newV1ObjectMetaBuilder = new V1ObjectMetaBuilder();
    V1ObjectMeta objectMeta = getMetadata(copy);
    if (objectMeta != null) {
      newV1ObjectMetaBuilder.withName(objectMeta.getName())
          .withNamespace(objectMeta.getNamespace())
          .withLabels(objectMeta.getLabels())
          .withAnnotations(objectMeta.getAnnotations())
          .withUid(objectMeta.getUid());
    }
    Reflect.on(copy).set(METADATA, newV1ObjectMetaBuilder.build());
    return Yaml.dump(copy);
  }

  @Override
  public void onDelete(ApiType resource, boolean finalStateUnknown) {
    handleMissingKindAndApiVersion(resource);
    logger.debug("Delete resource: {}, finalStateUnknown: {}", ResourceDetails.ofResource(resource), finalStateUnknown);
    V1OwnerReference controller = getController(resource);
    if (controller != null) {
      logger.debug("Skipping publish for resource deleted as it has controller: {}", controller);
    } else {
      K8sWatchEvent.Builder builder = K8sWatchEvent.newBuilder()
                                          .setType(K8sWatchEvent.Type.TYPE_DELETED)
                                          .setResourceRef(createObjectReference(resource))
                                          .setDescription(String.format("%s deleted", getKind()));
      if (!finalStateUnknown) {
        builder.setOldResourceVersion(getResourceVersion(resource)).setOldResourceYaml(yamlDump(resource));
      }
      publishMessage(builder.build(), HTimestamps.fromInstant(Instant.now()));
    }
  }

  private K8sObjectReference createObjectReference(ApiType resource) {
    K8sObjectReference.Builder builder = K8sObjectReference.newBuilder();
    V1ObjectMeta objectMeta = getMetadata(resource);
    if (objectMeta != null) {
      if (objectMeta.getNamespace() != null) {
        builder.setNamespace(objectMeta.getNamespace());
      }
      if (objectMeta.getName() != null) {
        builder.setName(objectMeta.getName());
      }
      if (objectMeta.getUid() != null) {
        builder.setUid(objectMeta.getUid());
      }
    }
    builder.setKind(getKind());
    return builder.build();
  }

  abstract String getKind();
  abstract String getApiVersion();

  private V1ObjectMeta getMetadata(ApiType resource) {
    return (V1ObjectMeta) ReflectionUtils.getFieldValue(resource, METADATA);
  }

  private String getResourceVersion(ApiType resource) {
    return Optional.ofNullable(getMetadata(resource)).map(V1ObjectMeta::getResourceVersion).orElse("");
  }

  private V1OwnerReference getController(ApiType resource) {
    V1ObjectMeta metadata = getMetadata(resource);
    if (metadata != null) {
      List<V1OwnerReference> ownerReferences = metadata.getOwnerReferences();
      if (ownerReferences != null) {
        for (V1OwnerReference ownerReference : ownerReferences) {
          if (Boolean.TRUE.equals(ownerReference.getController())) {
            return ownerReference;
          }
        }
      }
    }
    return null;
  }

  @Value
  @Builder
  private static class ResourceDetails {
    String kind;
    String namespace;
    String name;
    String uid;
    String resourceVersion;

    static ResourceDetails ofResource(Object resource) {
      Map<String, Object> fieldValues = ReflectionUtils.getFieldValues(resource, ImmutableSet.of("kind", METADATA));
      String kind = (String) fieldValues.get("kind");
      V1ObjectMeta objectMeta = (V1ObjectMeta) fieldValues.computeIfAbsent(METADATA, k -> new V1ObjectMeta());
      return ResourceDetails.builder()
          .kind(kind)
          .name(objectMeta.getName())
          .namespace(objectMeta.getNamespace())
          .uid(objectMeta.getUid())
          .resourceVersion(objectMeta.getResourceVersion())
          .build();
    }
  }
}
