package software.wings.service.impl.applicationmanifest;

import com.google.inject.Inject;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import org.mongodb.morphia.query.Query;
import software.wings.beans.appmanifest.HelmChart;
import software.wings.beans.appmanifest.HelmChart.HelmChartKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.applicationmanifest.HelmChartService;

@OwnedBy(HarnessTeam.CDC)
public class HelmChartServiceImpl implements HelmChartService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public HelmChart create(HelmChart helmChart) {
    String key = wingsPersistence.save(helmChart);
    return wingsPersistence.get(HelmChart.class, key);
  }

  @Override
  public PageResponse<HelmChart> listHelmChartsForService(PageRequest<HelmChart> pageRequest) {
    return wingsPersistence.query(HelmChart.class, pageRequest);
  }

  @Override
  public HelmChart get(String appId, String helmChartId) {
    return wingsPersistence.getWithAppId(HelmChart.class, appId, helmChartId);
  }

  @Override
  public void deleteByAppManifest(String appId, String applicationManifestId) {
    Query<HelmChart> query = wingsPersistence.createQuery(HelmChart.class)
                                 .filter(HelmChartKeys.appId, appId)
                                 .filter(HelmChartKeys.applicationManifestId, applicationManifestId);
    wingsPersistence.delete(query);
  }

  @Override
  public void pruneByApplicationManifest(String appId, String applicationManifestId) {
    deleteByAppManifest(appId, applicationManifestId);
  }
}
