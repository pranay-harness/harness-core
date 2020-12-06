package software.wings.delegatetasks.buildsource;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(Module._930_DELEGATE_TASKS)
public class BuildSourceResponse {
  private List<BuildDetails> buildDetails;
  private Set<String> toBeDeletedKeys;
  private boolean cleanup;
  private boolean stable;
}
