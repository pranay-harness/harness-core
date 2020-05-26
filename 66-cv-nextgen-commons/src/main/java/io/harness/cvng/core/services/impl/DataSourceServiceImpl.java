package io.harness.cvng.core.services.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.govern.Switch.unhandled;
import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.common.base.Charsets;
import com.google.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import io.harness.cvng.core.services.api.DataSourceService;
import io.harness.cvng.core.services.entities.MetricPack;
import io.harness.cvng.core.services.entities.MetricPack.MetricPackKeys;
import io.harness.cvng.models.DataSourceType;
import io.harness.data.structure.CollectionUtils;
import io.harness.persistence.HPersistence;
import io.harness.serializer.YamlUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataSourceServiceImpl implements DataSourceService {
  private static final String APPDYNAMICS_METRIC_PACKS_PATH = "/metric-packs/appdynamics";
  @Inject private HPersistence hPersistence;

  @Override
  public Collection<MetricPack> getMetricPacks(String accountId, String projectId, DataSourceType dataSourceType) {
    Map<String, MetricPack> metricPackMap = new HashMap<>();
    switch (dataSourceType) {
      case APP_DYNAMICS:
        final URL metricPackUrl = DataSourceService.class.getResource(APPDYNAMICS_METRIC_PACKS_PATH);
        final Collection<File> metricPackYamls = FileUtils.listFiles(new File(metricPackUrl.getFile()), null, false);
        YamlUtils yamlUtils = new YamlUtils();
        metricPackYamls.forEach(metricPackPath -> {
          try {
            final String metricPackYaml = FileUtils.readFileToString(metricPackPath, Charsets.UTF_8);
            final MetricPack metricPack = yamlUtils.read(metricPackYaml, new TypeReference<MetricPack>() {});
            metricPack.setAccountId(accountId);
            metricPack.setProjectId(projectId);
            metricPackMap.put(metricPack.getName(), metricPack);
          } catch (IOException e) {
            throw new IllegalStateException("Error reading metric packs", e);
          }
        });
        break;
      default:
        unhandled(dataSourceType);
        throw new IllegalStateException("Invalid dataSourceType " + dataSourceType);
    }

    List<MetricPack> metricPacksFromDb = hPersistence.createQuery(MetricPack.class, excludeAuthority)
                                             .filter(MetricPackKeys.projectId, projectId)
                                             .filter(MetricPackKeys.dataSourceType, dataSourceType)
                                             .asList();
    // add any additional metrics that are not present in the db (e.g. appdynamics started supporting a new metric type)
    metricPacksFromDb.stream()
        .filter(metricPackFromDb
            -> metricPackMap.containsKey(metricPackFromDb.getName())
                && !CollectionUtils.isEqualCollection(
                       metricPackFromDb.getMetrics(), metricPackMap.get(metricPackFromDb.getName()).getMetrics()))
        .forEach(metricPackFromDb -> {
          final MetricPack globalMetricPack = metricPackMap.get(metricPackFromDb.getName());
          globalMetricPack.getMetrics().removeAll(metricPackFromDb.getMetrics());

          if (isNotEmpty(globalMetricPack.getMetrics())) {
            metricPackFromDb.getMetrics().addAll(globalMetricPack.getMetrics());
          }
          metricPackMap.put(metricPackFromDb.getName(), metricPackFromDb);
        });
    return metricPackMap.values();
  }

  @Override
  public boolean saveMetricPacks(
      String accountId, String projectId, DataSourceType dataSourceType, List<MetricPack> metricPacks) {
    if (isEmpty(metricPacks)) {
      return false;
    }

    metricPacks.forEach(metricPack -> {
      metricPack.setAccountId(accountId);
      metricPack.setProjectId(projectId);
      metricPack.setDataSourceType(dataSourceType);
    });
    hPersistence.save(metricPacks);
    return true;
  }
}
