package io.harness.beans.steps;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.beans.repo.RepoConfiguration;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

@Data
@Value
@Builder
public class CIStepMetadata {
  private String uuid = generateUuid();
  private RepoConfiguration repoConfiguration;
}
