package io.harness.delegate.beans.connector.splunkconnector;

import io.harness.beans.DecryptableEntity;
import io.harness.connector.DelegateSelectable;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModelProperty;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SplunkConnectorDTO extends ConnectorConfigDTO implements DecryptableEntity, DelegateSelectable {
  @NotNull @NotBlank String splunkUrl;
  String username;
  @NotNull String accountId;
  Set<String> delegateSelectors;

  public String getSplunkUrl() {
    if (splunkUrl.endsWith("/")) {
      return splunkUrl;
    }
    return splunkUrl + "/";
  }

  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData passwordRef;

  @Override
  public List<DecryptableEntity> getDecryptableEntities() {
    return Collections.singletonList(this);
  }
}
