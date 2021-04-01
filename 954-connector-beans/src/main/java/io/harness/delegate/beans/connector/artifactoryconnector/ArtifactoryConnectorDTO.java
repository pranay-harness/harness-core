package io.harness.delegate.beans.connector.artifactoryconnector;

import io.harness.beans.DecryptableEntity;
import io.harness.connector.DelegateSelectable;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotBlank;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("ArtifactoryConnector")
public class ArtifactoryConnectorDTO extends ConnectorConfigDTO implements DelegateSelectable {
  @NotNull @NotBlank String artifactoryServerUrl;
  @Valid ArtifactoryAuthenticationDTO auth;
  Set<String> delegateSelectors;
  @Override
  public List<DecryptableEntity> getDecryptableEntities() {
    if (auth.getAuthType() == ArtifactoryAuthType.ANONYMOUS) {
      return null;
    }
    return Collections.singletonList(auth.getCredentials());
  }
}
