package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.common.EntityReference;
import io.harness.common.EntityReferenceHelper;

import java.util.Arrays;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(PIPELINE)
public class TriggerReference implements EntityReference {
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  String pipelineIdentifier;
  String identifier;

  @Override
  public String getFullyQualifiedName() {
    return EntityReferenceHelper.createFQN(
        Arrays.asList(accountIdentifier, orgIdentifier, projectIdentifier, pipelineIdentifier, identifier));
  }
}
