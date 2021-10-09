package software.wings.beans.appmanifest;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.data.structure.EmptyPredicate;

import software.wings.service.impl.ApplicationManifestServiceImpl;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class ManifestSummary {
  private String uuid;
  private String versionNo;
  private String name;
  private String source;
  private String appManifestName;

  public static ManifestSummary prepareSummaryFromHelmChart(HelmChart helmChart) {
    if (helmChart == null) {
      return null;
    }
    ManifestSummaryBuilder manifestSummaryBuilder =
        ManifestSummary.builder().uuid(helmChart.getUuid()).versionNo(helmChart.getVersion()).name(helmChart.getName());
    Map<String, String> metadata = helmChart.getMetadata();
    if (EmptyPredicate.isNotEmpty(helmChart.getMetadata())) {
      manifestSummaryBuilder.source(metadata.get(ApplicationManifestServiceImpl.CHART_URL));
    }
    return manifestSummaryBuilder.build();
  }
}
