package software.wings.beans.template.artifacts;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;

@JsonTypeName("CUSTOM")
@Value
@Builder
@JsonInclude(NON_NULL)
public class CustomArtifactSourceTemplate implements ArtifactSource {
  private String script;
  @Builder.Default private long timeoutSeconds = 60;
  private CustomRepositoryMapping customRepositoryMapping;
}
