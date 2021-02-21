package io.harness.delegate.beans.connector.gcpkmsconnector;

import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"credentials"})
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GcpKmsConnectorDTO extends ConnectorConfigDTO {
  private String projectId;
  private String region;
  private String keyRing;
  private String keyName;
  private char[] credentials;
  private boolean isDefault;
  @JsonIgnore private boolean harnessManaged;

  @Builder
  public GcpKmsConnectorDTO(
      String projectId, String region, String keyRing, String keyName, char[] credentials, boolean isDefault) {
    this.projectId = projectId;
    this.region = region;
    this.keyRing = keyRing;
    this.keyName = keyName;
    this.credentials = credentials;
    this.isDefault = isDefault;
  }

  @Override
  public List<DecryptableEntity> getDecryptableEntities() {
    return null;
  }
}
