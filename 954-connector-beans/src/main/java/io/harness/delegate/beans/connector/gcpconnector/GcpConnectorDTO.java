package io.harness.delegate.beans.connector.gcpconnector;

import static io.harness.ConnectorConstants.INHERIT_FROM_DELEGATE_TYPE_ERROR_MSG;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("GcpConnector")
public class GcpConnectorDTO extends ConnectorConfigDTO implements DelegateSelectable {
  @Valid GcpConnectorCredentialDTO credential;
  Set<String> delegateSelectors;
  @Override
  public List<DecryptableEntity> getDecryptableEntities() {
    if (credential.getGcpCredentialType() == GcpCredentialType.MANUAL_CREDENTIALS) {
      return Collections.singletonList(credential.getConfig());
    }
    return null;
  }

  @Override
  public void validate() {
    if (GcpCredentialType.INHERIT_FROM_DELEGATE.equals(credential.getGcpCredentialType())
        && isEmpty(delegateSelectors)) {
      throw new InvalidRequestException(INHERIT_FROM_DELEGATE_TYPE_ERROR_MSG);
    }
  }
}
