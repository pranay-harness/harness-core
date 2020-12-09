package io.harness.ng.core.dto.secrets;

import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.ng.core.models.TGTGenerationSpec;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "tgtGenerationMethod",
    visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = TGTKeyTabFilePathSpecDTO.class, name = "KeyTabFilePath")
  , @JsonSubTypes.Type(value = TGTPasswordSpecDTO.class, name = "Password")
})
public abstract class TGTGenerationSpecDTO {
  public abstract TGTGenerationSpec toEntity();
}
