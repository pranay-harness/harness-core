package software.wings.beans.appmanifest;

import static io.harness.data.structure.HasPredicate.hasSome;

import software.wings.service.impl.ApplicationManifestServiceImpl;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ManifestSummary {
  private String uuid;
  private String versionNo;
  private String name;
  private String source;

  public static ManifestSummary prepareSummaryFromHelmChart(HelmChart helmChart) {
    if (helmChart == null) {
      return null;
    }
    ManifestSummaryBuilder manifestSummaryBuilder =
        ManifestSummary.builder().uuid(helmChart.getUuid()).versionNo(helmChart.getVersion()).name(helmChart.getName());
    Map<String, String> metadata = helmChart.getMetadata();
    if (hasSome(helmChart.getMetadata())) {
      manifestSummaryBuilder.source(metadata.get(ApplicationManifestServiceImpl.CHART_URL));
    }
    return manifestSummaryBuilder.build();
  }
}
