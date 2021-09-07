package io.harness.cvng.beans.appd;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.DataCollectionRequest;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;
import io.harness.delegate.beans.cvng.appd.AppDynamicsUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@OwnedBy(CV)
public abstract class AppDynamicsDataCollectionRequest extends DataCollectionRequest<AppDynamicsConnectorDTO> {
  @Override
  public Map<String, String> collectionHeaders() {
    return AppDynamicsUtils.collectionHeaders(getConnectorConfigDTO());
  }

  @Override
  @JsonIgnore
  public String getBaseUrl() {
    return getConnectorConfigDTO().getControllerUrl();
  }
}
