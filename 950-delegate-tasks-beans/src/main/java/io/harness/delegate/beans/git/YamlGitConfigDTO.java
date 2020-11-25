package io.harness.delegate.beans.git;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.encryption.Scope;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class YamlGitConfigDTO {
  private String identifier;
  private String accountId;
  private String projectId;
  private String organizationId;
  private String gitConnectorId;
  private String repo;
  private String branch;
  // pair of identifier and folder name
  private List<RootFolder> rootFolders;
  private RootFolder defaultRootFolder;

  private String entityFQN;
  private Scope scope;

  @Data
  @FieldDefaults(level = AccessLevel.PRIVATE)
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonInclude(NON_NULL)
  @NoArgsConstructor
  public static class RootFolder {
    String identifier;
    String rootFolder;
    boolean enabled;

    @Builder(toBuilder = true)
    public RootFolder(String identifier, String rootFolder, boolean enabled) {
      this.identifier = identifier;
      this.rootFolder = rootFolder;
      this.enabled = enabled;
    }
  }
}
