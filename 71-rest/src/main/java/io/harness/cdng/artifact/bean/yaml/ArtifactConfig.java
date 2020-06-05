package io.harness.cdng.artifact.bean.yaml;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * This is Yaml POJO class which may contain expressions as well.
 * Used mainly for converter layer to store yaml.
 */
@JsonTypeInfo(use = NAME, property = "sourceType", include = PROPERTY, visible = true)
public interface ArtifactConfig {
  String getIdentifier();
  String getSourceType();
  Spec getSpec();
}
