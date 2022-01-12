/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.connector.cek8s;

import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.connector.CEFeatures;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel("CEKubernetesClusterConfig")
public class CEKubernetesClusterConfigDTO extends ConnectorConfigDTO {
  @NotNull @Valid String connectorRef;
  @NotEmpty(message = "At least one CE K8s Features should be enabled") List<CEFeatures> featuresEnabled;

  @Override
  public List<DecryptableEntity> getDecryptableEntities() {
    return null;
  }
}
