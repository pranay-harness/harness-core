package software.wings.service.intfc.applicationmanifest;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import software.wings.beans.appmanifest.HelmChart;
import software.wings.service.intfc.ownership.OwnedByApplicationManifest;

import java.util.List;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@OwnedBy(HarnessTeam.CDC)
public interface HelmChartService extends OwnedByApplicationManifest {
  HelmChart create(@Valid HelmChart helmChart);

  PageResponse<HelmChart> listHelmChartsForService(PageRequest<HelmChart> pageRequest);

  HelmChart get(String appId, String helmChartId);

  void deleteByAppManifest(String appId, @NotNull String applicationManifestId);

  List<HelmChart> listHelmChartsForAppManifest(@NotNull String accountId, @NotNull String appManifestId);

  boolean deleteHelmChartsByVersions(String accountId, String appManifestId, Set<String> toBeDeletedVersions);

  boolean addCollectedHelmCharts(String accountId, String appManifestId, @NotNull List<HelmChart> manifestsCollected);
}
