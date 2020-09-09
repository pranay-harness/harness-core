package software.wings.service.impl.applicationmanifest;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.HelmChart;
import software.wings.service.intfc.applicationmanifest.HelmChartService;

@OwnedBy(HarnessTeam.CDC)
public class HelmChartServiceImpl implements HelmChartService {
  @Override
  public HelmChart create(HelmChart helmChart) {
    return null;
  }

  @Override
  public PageResponse<HelmChart> listHelmChartsForService(PageRequest<HelmChart> pageRequest) {
    return null;
  }

  @Override
  public HelmChart get(String accountId, String helmChartId) {
    return null;
  }

  @Override
  public void deleteByAppManifest(ApplicationManifest applicationManifest) {
    // TODO
    throw new UnsupportedOperationException("Work in Progress");
  }

  @Override
  public void pruneByApplicationManifest(String appId, String applicationManifestId) {
    // TODO
    throw new UnsupportedOperationException("Work in Progress");
  }
}
