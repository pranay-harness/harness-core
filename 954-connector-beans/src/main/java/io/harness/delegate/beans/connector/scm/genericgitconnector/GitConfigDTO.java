package io.harness.delegate.beans.connector.scm.genericgitconnector;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.connector.DelegateSelectable;
import io.harness.connector.ManagerExecutable;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.ScmConnector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotBlank;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(HarnessTeam.DX)
public class GitConfigDTO extends ConnectorConfigDTO implements ScmConnector, DelegateSelectable, ManagerExecutable {
  @NotNull @JsonProperty("type") private GitAuthType gitAuthType;
  @NotNull @JsonProperty("connectionType") private GitConnectionType gitConnectionType;
  @NotNull @NotBlank String url;
  private String validationRepo;
  private String branchName;
  private Set<String> delegateSelectors;
  private Boolean executeOnManager;

  @JsonProperty("spec")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  @Valid
  @NotNull
  GitAuthenticationDTO gitAuth;

  @Builder
  public GitConfigDTO(GitAuthType gitAuthType, GitAuthenticationDTO gitAuth, GitConnectionType gitConnectionType,
      String url, String validationRepo, String branchName, Set<String> delegateSelectors, Boolean executeOnManager) {
    this.gitAuthType = gitAuthType;
    this.gitAuth = gitAuth;
    this.gitConnectionType = gitConnectionType;
    this.url = url;
    this.validationRepo = validationRepo;
    this.branchName = branchName;
    this.delegateSelectors = delegateSelectors;
    this.executeOnManager = executeOnManager;
  }

  @Override
  public List<DecryptableEntity> getDecryptableEntities() {
    return Collections.singletonList(gitAuth);
  }
}
