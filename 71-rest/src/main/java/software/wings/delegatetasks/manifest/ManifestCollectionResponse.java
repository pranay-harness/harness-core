package software.wings.delegatetasks.manifest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.beans.appmanifest.HelmChart;

import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManifestCollectionResponse {
  private List<HelmChart> helmCharts;
  private Set<String> toBeDeletedKeys;
  private boolean stable;
}
