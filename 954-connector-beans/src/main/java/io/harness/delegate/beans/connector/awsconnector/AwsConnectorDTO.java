package io.harness.delegate.beans.connector.awsconnector;

import static io.harness.delegate.beans.connector.awsconnector.AwsCredentialType.MANUAL_CREDENTIALS;

import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import java.util.Collections;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel("AwsConnector")
public class AwsConnectorDTO extends ConnectorConfigDTO {
  @Valid @NotNull AwsCredentialDTO credential;

  @Override
  public List<DecryptableEntity> getDecryptableEntities() {
    if (credential.getAwsCredentialType() == MANUAL_CREDENTIALS) {
      AwsManualConfigSpecDTO awsManualCredentials = (AwsManualConfigSpecDTO) credential.getConfig();
      return Collections.singletonList(awsManualCredentials);
    }
    return null;
  }
}
