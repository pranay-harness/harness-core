package io.harness.cvng.core.services.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.govern.Switch.unhandled;
import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.google.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.TimeSeriesThresholdActionType;
import io.harness.cvng.beans.TimeSeriesThresholdCriteria;
import io.harness.cvng.core.services.api.MetricPackService;
import io.harness.cvng.core.services.entities.MetricPack;
import io.harness.cvng.core.services.entities.MetricPack.MetricDefinition;
import io.harness.cvng.core.services.entities.MetricPack.MetricPackKeys;
import io.harness.cvng.core.services.entities.TimeSeriesThreshold;
import io.harness.cvng.core.services.entities.TimeSeriesThreshold.TimeSeriesThresholdKeys;
import io.harness.persistence.HPersistence;
import io.harness.serializer.YamlUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetricPackServiceImpl implements MetricPackService {
  static final List<String> APPDYNAMICS_METRICPACK_FILES =
      Lists.newArrayList("/metric-packs/appdynamics/business-transactions-pack.yml",
          "/metric-packs/appdynamics/quality-pack.yml", "/metric-packs/appdynamics/resource-pack.yml");
  @Inject private HPersistence hPersistence;

  @Override
  public List<MetricPack> getMetricPacks(String accountId, String projectIdentifier, DataSourceType dataSourceType) {
    List<MetricPack> metricPacksFromDb = hPersistence.createQuery(MetricPack.class, excludeAuthority)
                                             .filter(MetricPackKeys.projectIdentifier, projectIdentifier)
                                             .filter(MetricPackKeys.dataSourceType, dataSourceType)
                                             .asList();
    // TODO: ideally we would like to do this as a listener to creation of the project.
    if (isEmpty(metricPacksFromDb)) {
      final Map<String, MetricPack> metricPackDefinitionsFromYaml =
          getMetricPackDefinitionsFromYaml(accountId, projectIdentifier, dataSourceType);
      final ArrayList<MetricPack> metricPacks = Lists.newArrayList(metricPackDefinitionsFromYaml.values());
      hPersistence.save(metricPacks);
      return metricPacks;
    }

    metricPacksFromDb.forEach(metricPack -> metricPack.getMetrics().forEach(metricDefinition -> {
      if (metricDefinition.getFailFastHints() == null) {
        metricDefinition.setFailFastHints(Collections.emptyList());
      }
      if (metricDefinition.getIgnoreHints() == null) {
        metricDefinition.setIgnoreHints(Collections.emptyList());
      }
    }));
    return metricPacksFromDb;
  }

  private Map<String, MetricPack> getMetricPackDefinitionsFromYaml(
      String accountId, String projectIdentifier, DataSourceType dataSourceType) {
    Map<String, MetricPack> metricPacks = new HashMap<>();

    // TODO: when new datasource is on boarded we should stub this out to its own piece so that the switch statements do
    // not grow
    switch (dataSourceType) {
      case APP_DYNAMICS:
        YamlUtils yamlUtils = new YamlUtils();
        APPDYNAMICS_METRICPACK_FILES.forEach(metricPackPath -> {
          try {
            final String metricPackYaml =
                Resources.toString(MetricPackService.class.getResource(metricPackPath), Charsets.UTF_8);
            final MetricPack metricPack = yamlUtils.read(metricPackYaml, new TypeReference<MetricPack>() {});
            metricPack.setAccountId(accountId);
            metricPack.setProjectIdentifier(projectIdentifier);
            metricPack.setDataSourceType(dataSourceType);
            metricPack.getMetrics().forEach(metricDefinition -> {
              metricDefinition.setFailFastHints(Collections.emptyList());
              metricDefinition.setIgnoreHints(Collections.emptyList());
            });
            metricPacks.put(metricPack.getIdentifier(), metricPack);
          } catch (IOException e) {
            throw new IllegalStateException("Error reading metric packs", e);
          }
        });
        return metricPacks;
      default:
        unhandled(dataSourceType);
        throw new IllegalStateException("Invalid dataSourceType " + dataSourceType);
    }
  }

  @Override
  public boolean saveMetricPacks(
      String accountId, String projectIdentifier, DataSourceType dataSourceType, List<MetricPack> metricPacks) {
    if (isEmpty(metricPacks)) {
      return false;
    }

    final Map<String, MetricPack> metricPackDefinitions =
        getMetricPackDefinitionsFromYaml(accountId, projectIdentifier, dataSourceType);

    metricPacks.stream()
        .filter(metricPack -> metricPackDefinitions.containsKey(metricPack.getIdentifier()))
        .forEach(metricPack -> {
          final MetricPack metricPackDefinition = metricPackDefinitions.get(metricPack.getIdentifier());
          metricPack.setAccountId(accountId);
          metricPack.setProjectIdentifier(projectIdentifier);
          metricPack.setDataSourceType(dataSourceType);
          metricPack.getMetrics().forEach(metricDefinition -> {
            final MetricDefinition metricDefinitionFromYaml =
                metricPackDefinition.getMetrics()
                    .stream()
                    .filter(metric -> metricDefinition.getName().equals(metric.getName()))
                    .findFirst()
                    .orElse(null);
            if (metricDefinitionFromYaml != null) {
              metricDefinition.setPath(metricDefinitionFromYaml.getPath());
              metricDefinition.setValidationPath(metricDefinitionFromYaml.getValidationPath());
            }
          });
        });
    hPersistence.save(metricPacks);
    return true;
  }

  @Override
  public List<TimeSeriesThreshold> getMetricPackThresholds(
      String accountId, String projectIdentifier, String metricPackIdentifier, DataSourceType dataSourceType) {
    List<TimeSeriesThreshold> timeSeriesThresholdsFromDb =
        hPersistence.createQuery(TimeSeriesThreshold.class, excludeAuthority)
            .filter(TimeSeriesThresholdKeys.accountId, accountId)
            .filter(TimeSeriesThresholdKeys.projectIdentifier, projectIdentifier)
            .filter(TimeSeriesThresholdKeys.metricPackIdentifier, metricPackIdentifier)
            .filter(TimeSeriesThresholdKeys.dataSourceType, dataSourceType)
            .asList();

    // TODO: this should be done at the time of project creation
    if (isEmpty(timeSeriesThresholdsFromDb)) {
      return createDefaultIgnoreThresholds(accountId, projectIdentifier, metricPackIdentifier, dataSourceType);
    }

    return timeSeriesThresholdsFromDb;
  }

  private List<TimeSeriesThreshold> createDefaultIgnoreThresholds(
      String accountId, String projectIdentifier, String metricPackIdentifier, DataSourceType dataSourceType) {
    MetricPack metricPack = hPersistence.createQuery(MetricPack.class, excludeAuthority)
                                .filter(MetricPackKeys.accountId, accountId)
                                .filter(MetricPackKeys.projectIdentifier, projectIdentifier)
                                .filter(MetricPackKeys.identifier, metricPackIdentifier)
                                .filter(MetricPackKeys.dataSourceType, dataSourceType)
                                .get();

    Preconditions.checkNotNull(
        metricPack, "No metric pack found for project and pack ", projectIdentifier, metricPackIdentifier);

    List<TimeSeriesThreshold> timeSeriesThresholds = new ArrayList<>();
    metricPack.getMetrics().forEach(metricDefinition -> {
      final List<TimeSeriesThresholdCriteria> thresholds = metricDefinition.getType().getThresholds();
      thresholds.forEach(threshold -> {
        timeSeriesThresholds.add(TimeSeriesThreshold.builder()
                                     .accountId(accountId)
                                     .projectIdentifier(projectIdentifier)
                                     .dataSourceType(dataSourceType)
                                     .metricPackIdentifier(metricPackIdentifier)
                                     .metricName(metricDefinition.getName())
                                     .action(TimeSeriesThresholdActionType.IGNORE)
                                     .criteria(threshold)
                                     .build());
      });
    });
    saveMetricPackThreshold(accountId, projectIdentifier, dataSourceType, timeSeriesThresholds);
    return timeSeriesThresholds;
  }

  @Override
  public List<String> saveMetricPackThreshold(String accountId, String projectIdentifier, DataSourceType dataSourceType,
      List<TimeSeriesThreshold> timeSeriesThresholds) {
    timeSeriesThresholds.forEach(timeSeriesThreshold -> {
      timeSeriesThreshold.setAccountId(accountId);
      timeSeriesThreshold.setProjectIdentifier(projectIdentifier);
      timeSeriesThreshold.setDataSourceType(dataSourceType);
    });
    return hPersistence.save(timeSeriesThresholds);
  }

  @Override
  public boolean deleteMetricPackThresholds(String accountId, String projectIdentifier, String thresholdId) {
    return hPersistence.delete(TimeSeriesThreshold.class, thresholdId);
  }

  @Override
  public void populateValidationPaths(
      String accountId, String projectIdentifier, DataSourceType dataSourceType, MetricPack metricPack) {
    final List<MetricPack> metricPacksForProject = getMetricPacks(accountId, projectIdentifier, dataSourceType);
    final MetricPack metricPackForProject =
        metricPacksForProject.stream()
            .filter(metricPackProject -> metricPackProject.getIdentifier().equals(metricPack.getIdentifier()))
            .findFirst()
            .orElse(null);
    if (metricPackForProject == null) {
      return;
    }

    metricPack.getMetrics().forEach(metricDefinition -> {
      final MetricDefinition metricDefinitionFromProject =
          metricPackForProject.getMetrics()
              .stream()
              .filter(metric -> metric.getName().equals(metricDefinition.getName()))
              .findFirst()
              .orElse(null);
      if (metricDefinitionFromProject != null) {
        metricDefinition.setValidationPath(metricDefinitionFromProject.getValidationPath());
      }
    });
  }
}
