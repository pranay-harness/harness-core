package io.harness.delegate.beans.connector.gitops;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@ApiModel("GitopsProviderResponse")
@OwnedBy(HarnessTeam.GITOPS)
public class GitOpsProviderResponseDTO extends GitOpsProviderDTO {}
