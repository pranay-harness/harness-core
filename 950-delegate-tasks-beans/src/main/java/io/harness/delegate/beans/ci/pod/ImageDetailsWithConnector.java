package io.harness.delegate.beans.ci.pod;

import io.harness.expression.ExpressionReflectionUtils.NestedAnnotationResolver;
import io.harness.k8s.model.ImageDetails;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageDetailsWithConnector implements NestedAnnotationResolver {
  @NotNull private ConnectorDetails imageConnectorDetails;
  @NotNull private ImageDetails imageDetails;
}
