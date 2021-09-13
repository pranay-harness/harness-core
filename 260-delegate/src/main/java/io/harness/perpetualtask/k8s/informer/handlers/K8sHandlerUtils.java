package io.harness.perpetualtask.k8s.informer.handlers;

import io.harness.reflection.ReflectionUtils;

import com.google.common.collect.ImmutableSet;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1ObjectMetaBuilder;
import io.kubernetes.client.util.Yaml;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.joor.Reflect;

@Slf4j
public class K8sHandlerUtils<ApiType extends KubernetesObject> {
  private static final String METADATA = "metadata";
  public static final Integer VERSION = 1;

  public ApiType clone(ApiType resource) {
    try {
      @SuppressWarnings("unchecked") ApiType copy = (ApiType) Yaml.load(Yaml.dump(resource));
      return copy;
    } catch (IOException e) {
      log.warn("Serialization round trip should clone", e);
      return resource;
    }
  }

  public String yamlDump(ApiType resource) {
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

  public V1ObjectMeta getMetadata(ApiType resource) {
    return (V1ObjectMeta) ReflectionUtils.getFieldValue(resource, METADATA);
  }

  public String getResourceVersion(ApiType resource) {
    return Optional.ofNullable(getMetadata(resource)).map(V1ObjectMeta::getResourceVersion).orElse("");
  }

  @Value
  @Builder
  public static class ResourceDetails {
    String kind;
    String namespace;
    String name;
    String uid;
    String resourceVersion;

    public static ResourceDetails ofResource(Object resource) {
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
