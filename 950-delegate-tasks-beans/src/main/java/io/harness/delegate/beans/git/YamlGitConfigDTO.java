/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.git;

import static io.harness.annotations.dev.HarnessTeam.DX;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.encryption.Scope;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import javax.validation.constraints.NotNull;
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
@OwnedBy(DX)
public class YamlGitConfigDTO {
  private String identifier;
  private String name;
  private String accountIdentifier;
  private String projectIdentifier;
  private String organizationIdentifier;
  private String gitConnectorRef;
  @NotNull private ConnectorType gitConnectorType;
  private String repo;
  @NotNull private String branch;
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
