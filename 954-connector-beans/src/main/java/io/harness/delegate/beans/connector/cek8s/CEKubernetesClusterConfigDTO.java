package io.harness.delegate.beans.connector.cek8s;

import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel("CEKubernetesClusterConfig")
public class CEKubernetesClusterConfigDTO extends ConnectorConfigDTO {
  @NotNull @Valid String connectorRef;

  @Override
  public List<DecryptableEntity> getDecryptableEntities() {
    return null;
  }
}
