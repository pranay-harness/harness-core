package software.wings.delegatetasks.manifest;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.appmanifest.HelmChart;

import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TargetModule(Module._930_DELEGATE_TASKS)
public class ManifestCollectionResponse {
  private List<HelmChart> helmCharts;
  private Set<String> toBeDeletedKeys;
  private boolean stable;
}
