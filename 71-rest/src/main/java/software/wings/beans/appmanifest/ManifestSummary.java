package software.wings.beans.appmanifest;

import io.harness.data.structure.EmptyPredicate;
import lombok.Builder;
import lombok.Data;
import software.wings.service.impl.ApplicationManifestServiceImpl;

import java.util.Map;

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
    if (EmptyPredicate.isNotEmpty(helmChart.getMetadata())) {
      manifestSummaryBuilder.source(metadata.get(ApplicationManifestServiceImpl.CHART_URL));
    }
    return manifestSummaryBuilder.build();
  }
}
