package software.wings.beans.template.artifacts;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import software.wings.beans.template.BaseTemplate;

@JsonTypeName("ARTIFACT_SOURCE")
@Value
@Builder
@JsonInclude(NON_NULL)
public class ArtifactSourceTemplate implements BaseTemplate {
  ArtifactSource artifactSource;
}