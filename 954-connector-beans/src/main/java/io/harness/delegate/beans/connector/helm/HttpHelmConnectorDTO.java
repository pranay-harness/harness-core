package io.harness.delegate.beans.connector.helm;

import static io.harness.delegate.beans.connector.helm.HttpHelmAuthType.ANONYMOUS;

import io.harness.beans.DecryptableEntity;
import io.harness.connector.DelegateSelectable;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(name = "HttpHelmConnector", description = "This contains http helm connector details")
public class HttpHelmConnectorDTO extends ConnectorConfigDTO implements DelegateSelectable {
  @NotNull @NotBlank String helmRepoUrl;
  @Valid HttpHelmAuthenticationDTO auth;
  Set<String> delegateSelectors;

  @Override
  public List<DecryptableEntity> getDecryptableEntities() {
    if (auth.getAuthType() == ANONYMOUS) {
      return Collections.emptyList();
    }
    return Collections.singletonList(auth.getCredentials());
  }
}
