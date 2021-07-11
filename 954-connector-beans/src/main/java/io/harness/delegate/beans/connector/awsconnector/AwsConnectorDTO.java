package io.harness.delegate.beans.connector.awsconnector;

import static io.harness.ConnectorConstants.INHERIT_FROM_DELEGATE_TYPE_ERROR_MSG;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.beans.connector.awsconnector.AwsCredentialType.MANUAL_CREDENTIALS;

import io.harness.beans.DecryptableEntity;
import io.harness.connector.DelegateSelectable;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.exception.InvalidRequestException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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
public class AwsConnectorDTO extends ConnectorConfigDTO implements DelegateSelectable {
  @Valid @NotNull AwsCredentialDTO credential;
  Set<String> delegateSelectors;

  @Override
  public List<DecryptableEntity> getDecryptableEntities() {
    if (credential.getAwsCredentialType() == MANUAL_CREDENTIALS) {
      AwsManualConfigSpecDTO awsManualCredentials = (AwsManualConfigSpecDTO) credential.getConfig();
      return Collections.singletonList(awsManualCredentials);
    }
    return null;
  }

  @Override
  public void validate() {
    if (AwsCredentialType.INHERIT_FROM_DELEGATE.equals(credential.getAwsCredentialType())
        && isEmpty(delegateSelectors)) {
      throw new InvalidRequestException(INHERIT_FROM_DELEGATE_TYPE_ERROR_MSG);
    }
  }
}
