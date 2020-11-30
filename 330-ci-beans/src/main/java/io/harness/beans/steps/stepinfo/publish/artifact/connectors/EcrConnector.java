package io.harness.beans.steps.stepinfo.publish.artifact.connectors;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("ecrConnector")
public class EcrConnector implements ArtifactConnector {
  @NotNull private String connectorRef;
  @NotNull private String location;
  @NotNull private String region;
  @Builder.Default @NotNull Type type = Type.ECR;
}
