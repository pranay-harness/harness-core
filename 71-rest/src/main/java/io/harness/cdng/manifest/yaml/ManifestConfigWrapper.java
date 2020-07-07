package io.harness.cdng.manifest.yaml;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.WRAPPER_OBJECT;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.yaml.core.intfc.WithIdentifier;

import java.io.Serializable;

@JsonTypeInfo(use = NAME, include = WRAPPER_OBJECT)
public interface ManifestConfigWrapper extends WithIdentifier, Serializable {
  ManifestAttributes getManifestAttributes();
}
