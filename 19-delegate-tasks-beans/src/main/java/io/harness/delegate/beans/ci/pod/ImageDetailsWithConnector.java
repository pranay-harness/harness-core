package io.harness.delegate.beans.ci.pod;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.k8s.model.ImageDetails;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageDetailsWithConnector {
  @NotNull private ConnectorDetails imageConnectorDetails;
  @NotNull private ImageDetails imageDetails;
}
