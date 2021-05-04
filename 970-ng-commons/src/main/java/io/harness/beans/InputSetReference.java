package io.harness.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.common.EntityReference;
import io.harness.common.EntityReferenceHelper;

import java.util.Arrays;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
public class InputSetReference implements EntityReference {
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  String pipelineIdentifier;
  // inputSet identifier
  String identifier;
  String repoIdentifier;
  String branch;
  Boolean isDefault;

  @Override
  public String getFullyQualifiedName() {
    return EntityReferenceHelper.createFQN(
        Arrays.asList(accountIdentifier, orgIdentifier, projectIdentifier, pipelineIdentifier, identifier));
  }

  @Override
  public Boolean isDefault() {
    return isDefault;
  }
}
