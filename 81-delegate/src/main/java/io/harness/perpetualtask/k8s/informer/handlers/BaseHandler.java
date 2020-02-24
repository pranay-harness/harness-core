package io.harness.perpetualtask.k8s.informer.handlers;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Timestamp;

import io.harness.event.client.EventPublisher;
import io.harness.grpc.utils.HTimestamps;
import io.harness.perpetualtask.k8s.informer.ClusterDetails;
import io.harness.perpetualtask.k8s.watch.K8sObjectReference;
import io.harness.perpetualtask.k8s.watch.K8sWatchEvent;
import io.harness.reflection.ReflectionUtils;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1OwnerReference;
import io.kubernetes.client.util.Yaml;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.joor.Reflect;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public abstract class BaseHandler<ApiType> implements ResourceEventHandler<ApiType> {
  private final EventPublisher eventPublisher;

  private final K8sWatchEvent clusterDetailsProto;

  public BaseHandler(EventPublisher eventPublisher, ClusterDetails clusterDetails) {
    this.eventPublisher = eventPublisher;
    this.clusterDetailsProto = K8sWatchEvent.newBuilder()
                                   .setClusterId(clusterDetails.getClusterId())
                                   .setCloudProviderId(clusterDetails.getCloudProviderId())
                                   .setClusterName(clusterDetails.getClusterName())
                                   .build();
  }

  protected void publishMessage(K8sWatchEvent resourceDetailsProto, Timestamp occurredAt) {
    eventPublisher.publishMessage(
        K8sWatchEvent.newBuilder(clusterDetailsProto).mergeFrom(resourceDetailsProto).build(), occurredAt);
  }

  @Override
  public void onAdd(ApiType resource) {
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

  @Override
  public void onUpdate(ApiType oldResource, ApiType newResource) {
    ResourceDetails oldResourceDetails = ResourceDetails.ofResource(oldResource);
    ResourceDetails newResourceDetails = ResourceDetails.ofResource(newResource);
    logger.debug("Resource: {} updated from {} to {}", oldResourceDetails, oldResourceDetails.getResourceVersion(),
        newResourceDetails.getResourceVersion());
    // Publish change event only if the spec changes, and the resource does not have a controlling owner (in which case
    // the change will be captured in owner)
    boolean specChanged = isSpecChanged(oldResource, newResource);
    V1OwnerReference controller = getController(oldResource);
    K8sObjectReference objectReference = createObjectReference(oldResource);
    logger.debug("Updated Resource: {}, SpecChanged: {}, controller:{}", objectReference, specChanged, controller);
    if (controller != null) {
      logger.debug("Skipping publish for resource updated as it has controller: {}", controller);
    } else if (!specChanged) {
      logger.debug("Skipping publish for resource updated since no spec change");
    } else {
      publishMessage(K8sWatchEvent.newBuilder()
                         .setType(K8sWatchEvent.Type.TYPE_UPDATED)
                         .setResourceRef(objectReference)
                         .setOldResourceVersion(getResourceVersion(oldResource))
                         .setOldResourceYaml(yamlDump(oldResource))
                         .setNewResourceVersion(getResourceVersion(newResource))
                         .setNewResourceYaml(yamlDump(newResource))
                         .setDescription(String.format("%s updated", getKind()))
                         .build(),
          HTimestamps.fromInstant(Instant.now()));
    }
  }

  private String yamlDump(ApiType resource) {
    Reflect.on(resource).set("status", null);
    return Yaml.dump(resource);
  }

  private boolean isSpecChanged(ApiType oldResource, ApiType newResource) {
    return ObjectUtils.notEqual(getSpec(oldResource), getSpec(newResource));
  }

  @Override
  public void onDelete(ApiType resource, boolean finalStateUnknown) {
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

  private V1ObjectMeta getMetadata(ApiType resource) {
    return (V1ObjectMeta) ReflectionUtils.getFieldValue(resource, "metadata");
  }

  private String getResourceVersion(ApiType resource) {
    return Optional.ofNullable(getMetadata(resource)).map(V1ObjectMeta::getResourceVersion).orElse("");
  }

  private Object getSpec(Object resource) {
    return ReflectionUtils.getFieldValue(resource, "spec");
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
      Map<String, Object> fieldValues = ReflectionUtils.getFieldValues(resource, ImmutableSet.of("kind", "metadata"));
      String kind = (String) fieldValues.get("kind");
      V1ObjectMeta objectMeta = (V1ObjectMeta) fieldValues.computeIfAbsent("metadata", k -> new V1ObjectMeta());
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
