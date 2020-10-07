package software.wings.service.impl.applicationmanifest;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;

import com.google.inject.Inject;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.data.structure.EmptyPredicate;
import org.mongodb.morphia.query.Query;
import software.wings.beans.appmanifest.HelmChart;
import software.wings.beans.appmanifest.HelmChart.HelmChartKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.applicationmanifest.HelmChartService;

import java.util.List;
import java.util.Set;

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
  public List<HelmChart> listHelmChartsForAppManifest(String accountId, String appManifestId) {
    return wingsPersistence.createQuery(HelmChart.class)
        .filter(HelmChartKeys.accountId, accountId)
        .filter(HelmChartKeys.applicationManifestId, appManifestId)
        .asList();
  }

  @Override
  public void deleteByAppManifest(String appId, String applicationManifestId) {
    Query<HelmChart> query = wingsPersistence.createQuery(HelmChart.class)
                                 .filter(HelmChartKeys.appId, appId)
                                 .filter(HelmChartKeys.applicationManifestId, applicationManifestId);
    wingsPersistence.delete(query);
  }

  @Override
  public boolean deleteHelmChartsByVersions(String accountId, String appManifestId, Set<String> toBeDeletedVersions) {
    Query<HelmChart> query = wingsPersistence.createQuery(HelmChart.class)
                                 .filter(HelmChartKeys.accountId, accountId)
                                 .filter(HelmChartKeys.applicationManifestId, appManifestId)
                                 .field(HelmChartKeys.chartVersion)
                                 .in(toBeDeletedVersions);
    return wingsPersistence.delete(query);
  }

  @Override
  public boolean addCollectedHelmCharts(String accountId, String appManifestId, List<HelmChart> manifestsCollected) {
    List<HelmChart> helmCharts = wingsPersistence.createQuery(HelmChart.class)
                                     .filter(HelmChartKeys.accountId, accountId)
                                     .filter(HelmChartKeys.applicationManifestId, appManifestId)
                                     .project(HelmChartKeys.chartVersion, true)
                                     .asList();

    List<String> versionsPresent = helmCharts.stream().map(HelmChart::getChartVersion).collect(toList());

    List<HelmChart> newHelmCharts = manifestsCollected.stream()
                                        .filter(helmChart -> !versionsPresent.contains(helmChart.getChartVersion()))
                                        .collect(toList());
    if (EmptyPredicate.isEmpty(newHelmCharts)) {
      return true;
    }

    List<String> savedHelmCharts = wingsPersistence.save(newHelmCharts);
    return isNotEmpty(savedHelmCharts);
  }

  @Override
  public void pruneByApplicationManifest(String appId, String applicationManifestId) {
    deleteByAppManifest(appId, applicationManifestId);
  }
}
