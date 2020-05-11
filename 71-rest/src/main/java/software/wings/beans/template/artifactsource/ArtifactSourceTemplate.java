package software.wings.beans.template.artifactsource;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;
import software.wings.beans.template.BaseTemplate;

@OwnedBy(CDC)
@JsonTypeName("ARTIFACT_SOURCE")
@Value
@Builder
@JsonInclude(NON_NULL)
public class ArtifactSourceTemplate implements BaseTemplate {
  ArtifactSource artifactSource;
}