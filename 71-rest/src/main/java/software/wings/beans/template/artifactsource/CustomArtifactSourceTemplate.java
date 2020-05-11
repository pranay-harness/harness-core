package software.wings.beans.template.artifactsource;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotations.dev.OwnedBy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(CDC)
@JsonTypeName("CUSTOM")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class CustomArtifactSourceTemplate implements ArtifactSource {
  private String script;
  @Builder.Default private String timeoutSeconds = "60";
  private CustomRepositoryMapping customRepositoryMapping;
}
