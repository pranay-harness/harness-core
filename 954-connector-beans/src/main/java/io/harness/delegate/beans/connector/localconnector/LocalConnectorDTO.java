package io.harness.delegate.beans.connector.localconnector;

import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LocalConnectorDTO extends ConnectorConfigDTO {
  private boolean isDefault;
  @JsonIgnore private boolean harnessManaged;

  @Builder
  public LocalConnectorDTO(boolean isDefault) {
    this.isDefault = isDefault;
  }

  @Override
  public DecryptableEntity getDecryptableEntity() {
    return null;
  }
}
