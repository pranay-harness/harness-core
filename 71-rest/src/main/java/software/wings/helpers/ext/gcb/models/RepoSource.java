package software.wings.helpers.ext.gcb.models;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.harness.annotations.dev.OwnedBy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@Data
@Builder
@OwnedBy(CDC)
@JsonInclude(NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RepoSource {
  @Nullable private String projectId;
  @Nullable private String repoName;
  @Nullable private String dir;
  @Nullable private Boolean invertRegex;
  @Nullable private Map<String, String> substitutions;
  @Nullable private String branchName;
  @Nullable private String tagName;
  @Nullable private String commitSha;
}
