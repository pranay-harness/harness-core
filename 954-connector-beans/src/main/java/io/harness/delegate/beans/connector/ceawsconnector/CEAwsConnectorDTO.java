/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.connector.ceawsconnector;

import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.connector.CEFeatures;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.awsconnector.CrossAccountAccessDTO;

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
@ApiModel("CEAwsConnector")
public class CEAwsConnectorDTO extends ConnectorConfigDTO {
  @NotNull @Valid CrossAccountAccessDTO crossAccountAccess;
  @Valid AwsCurAttributesDTO curAttributes;
  String awsAccountId;
  @NotEmpty(message = "FeaturesEnabled can't be empty") List<CEFeatures> featuresEnabled;

  @Override
  public List<DecryptableEntity> getDecryptableEntities() {
    return null;
  }
}
