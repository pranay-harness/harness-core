package io.harness.pms.cdng.manifest.yaml;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.WRAPPER_OBJECT;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.beans.WithIdentifier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.Serializable;

@JsonTypeInfo(use = NAME, include = WRAPPER_OBJECT)
public interface ManifestConfigWrapperPms extends WithIdentifier, Serializable {
  @JsonIgnore ManifestAttributesPms getManifestAttributes();
}
