package io.harness.beans;

import io.harness.common.EntityReference;
import io.harness.common.EntityReferenceHelper;

import java.util.Arrays;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InputSetReference implements EntityReference {
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  String pipelineIdentifier;
  // inputSet identifier
  String identifier;

  @Override
  public String getFullyQualifiedName() {
    return EntityReferenceHelper.createFQN(
        Arrays.asList(accountIdentifier, orgIdentifier, projectIdentifier, pipelineIdentifier, identifier));
  }
}
