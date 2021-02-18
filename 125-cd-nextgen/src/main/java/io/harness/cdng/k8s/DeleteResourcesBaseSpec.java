package io.harness.cdng.k8s;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
public interface DeleteResourcesBaseSpec {
  DeleteResourcesType getType();
  @JsonInclude(JsonInclude.Include.NON_EMPTY) String getResourceNames();
  @JsonInclude(JsonInclude.Include.NON_EMPTY) String getManifestPaths();
  @JsonInclude(JsonInclude.Include.NON_NULL) Boolean getDeleteNamespace();
  @JsonInclude(JsonInclude.Include.NON_NULL) Boolean getAllManifestPaths();
}
