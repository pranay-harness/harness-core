package io.harness.beans.steps.stepinfo.publish.artifact.connectors;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("s3Connector")
public class S3Connector implements ArtifactConnector {
  @NotNull private String connectorRef;
  @NotNull private String location;
  @Builder.Default @NotNull Type type = Type.S3;
}
