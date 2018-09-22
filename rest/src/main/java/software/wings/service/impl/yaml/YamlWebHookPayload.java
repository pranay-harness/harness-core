package software.wings.service.impl.yaml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class YamlWebHookPayload {
  @NotNull private List<Commit> commits;
  @JsonProperty(value = "head_commit") @NotEmpty private Commit headCommit;
  @NotEmpty private String ref;
  @Valid private Repository repository;

  @Data
  @Builder
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Commit {
    @NotEmpty private String id;
    @NotEmpty private String message;
    @NotEmpty private String timestamp;
    @JsonProperty(value = "tree_id") @NotEmpty private String treeId;
    @NotEmpty private String url;
    @NotNull private List<String> added;
  }

  @Data
  @Builder
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Repository {
    @JsonProperty(value = "clone_url") @NotEmpty private String cloneUrl;
  }
}
