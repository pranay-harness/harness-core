package io.harness.delegate.beans.connector.awskmsconnector;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import lombok.*;

import javax.validation.Valid;
import java.util.List;

import static io.harness.annotations.dev.HarnessTeam.PL;

@OwnedBy(PL)
@Getter
@Setter
@NoArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AwsKmsConnectorDTO extends ConnectorConfigDTO {
  @Valid
  AwsKmsConnectorCredentialDTO credential;

  private String name;
  private String kmsArn;
  private String region;
  private boolean isDefault;
  @JsonIgnore private boolean harnessManaged;

  @Builder
  public AwsKmsConnectorDTO(String name, String kmsArn, String region, AwsKmsConnectorCredentialDTO credential, boolean isDefault) {
    this.name = name;
    this.kmsArn = kmsArn;
    this.region = region;
    this.credential = credential;
    this.isDefault = isDefault;
  }

  @Override
  public List<DecryptableEntity> getDecryptableEntities() {
    return null;
  }

  public void validate() {
    //TODO: Shashank: Add validations here
  }
}
