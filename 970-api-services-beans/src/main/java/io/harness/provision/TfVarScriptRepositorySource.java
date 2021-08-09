package io.harness.provision;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode
public class TfVarScriptRepositorySource implements TfVarSource {
  private List<String> tfVarFilePaths;

  @Override
  public TfVarSourceType getTfVarSourceType() {
    return TfVarSourceType.SCRIPT_REPOSITORY;
  }
}
