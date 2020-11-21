package io.harness.cvng.core.services.impl;

import static io.harness.cvng.core.services.CVNextGenConstants.ERRORS_PACK_IDENTIFIER;
import static io.harness.cvng.core.services.CVNextGenConstants.INFRASTRUCTURE_PACK_IDENTIFIER;
import static io.harness.cvng.core.services.CVNextGenConstants.PERFORMANCE_PACK_IDENTIFIER;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.govern.Switch.unhandled;
import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.MetricPackDTO;
import io.harness.cvng.beans.TimeSeriesThresholdActionType;
import io.harness.cvng.beans.TimeSeriesThresholdCriteria;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.entities.MetricPack.MetricDefinition;
import io.harness.cvng.core.entities.MetricPack.MetricPackKeys;
import io.harness.cvng.core.entities.TimeSeriesThreshold;
import io.harness.cvng.core.entities.TimeSeriesThreshold.TimeSeriesThresholdKeys;
import io.harness.cvng.core.services.api.MetricPackService;
import io.harness.persistence.HPersistence;
import io.harness.serializer.YamlUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MetricPackServiceImpl implements MetricPackService {
  static final List<String> APPDYNAMICS_METRICPACK_FILES =
      Lists.newArrayList("/appdynamics/metric-packs/peformance-pack.yml", "/appdynamics/metric-packs/quality-pack.yml",
          "/appdynamics/metric-packs/infrastructure-pack.yml");
  private static final URL APPDYNAMICS_PERFORMANCE_PACK_DSL_PATH =
      MetricPackServiceImpl.class.getResource("/appdynamics/dsl/performance-pack.datacollection");
  public static final String APPDYNAMICS_PERFORMANCE_PACK_DSL;
  private static final URL APPDYNAMICS_QUALITY_PACK_DSL_PATH =
      MetricPackServiceImpl.class.getResource("/appdynamics/dsl/quality-pack.datacollection");
  public static final String APPDYNAMICS_QUALITY_PACK_DSL;
  private static final URL APPDYNAMICS_INFRASTRUCTURE_PACK_DSL_PATH =
      MetricPackServiceImpl.class.getResource("/appdynamics/dsl/infrastructure-pack.datacollection");
  public static final String APPDYNAMICS_INFRASTRUCTURE_PACK_DSL;
  static {
    String peformancePackDsl = null;
    String qualityPackDsl = null;
    String resourcePackDsl = null;
    try {
      peformancePackDsl = Resources.toString(APPDYNAMICS_PERFORMANCE_PACK_DSL_PATH, Charsets.UTF_8);
      qualityPackDsl = Resources.toString(APPDYNAMICS_QUALITY_PACK_DSL_PATH, Charsets.UTF_8);
      resourcePackDsl = Resources.toString(APPDYNAMICS_INFRASTRUCTURE_PACK_DSL_PATH, Charsets.UTF_8);
    } catch (Exception e) {
      // TODO: this should throw an exception but we risk delegate not starting up. We can remove this log term and
      // throw and exception once things stabilize
      log.error("Invalid metric pack dsl path", e);
    }
    APPDYNAMICS_PERFORMANCE_PACK_DSL = peformancePackDsl;
    APPDYNAMICS_QUALITY_PACK_DSL = qualityPackDsl;
    APPDYNAMICS_INFRASTRUCTURE_PACK_DSL = resourcePackDsl;
  }

  @Inject private HPersistence hPersistence;

  @Override
  public List<MetricPackDTO> getMetricPacks(DataSourceType dataSourceType, String accountId, String projectIdentifier) {
    return getMetricPacks(accountId, projectIdentifier, dataSourceType)
        .stream()
        .map(MetricPack::toDTO)
        .collect(Collectors.toList());
  }

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
      if (metricDefinition.getThresholds() == null) {
        metricDefinition.setThresholds(Collections.emptyList());
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
            metricPack.getMetrics().forEach(
                metricDefinition -> metricDefinition.setThresholds(Collections.emptyList()));
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
      thresholds.forEach(threshold
          -> timeSeriesThresholds.add(TimeSeriesThreshold.builder()
                                          .accountId(accountId)
                                          .projectIdentifier(projectIdentifier)
                                          .dataSourceType(dataSourceType)
                                          .metricType(metricDefinition.getType())
                                          .metricPackIdentifier(metricPackIdentifier)
                                          .metricName(metricDefinition.getName())
                                          .action(TimeSeriesThresholdActionType.IGNORE)
                                          .criteria(threshold)
                                          .build()));
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
  public void populatePaths(
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
    populateDataCollectionDsl(dataSourceType, metricPack);
    metricPack.getMetrics().forEach(metricDefinition -> {
      final MetricDefinition metricDefinitionFromProject =
          metricPackForProject.getMetrics()
              .stream()
              .filter(metric -> metric.getName().equals(metricDefinition.getName()))
              .findFirst()
              .orElse(null);
      if (metricDefinitionFromProject != null) {
        metricDefinition.setPath(metricDefinitionFromProject.getPath());
        metricDefinition.setValidationPath(metricDefinitionFromProject.getValidationPath());
      }
    });
  }
  @Override
  public void populateDataCollectionDsl(DataSourceType dataSourceType, MetricPack metricPack) {
    switch (dataSourceType) {
      case APP_DYNAMICS:
        metricPack.setDataCollectionDsl(getAppdynamicsMetricPackDsl(metricPack));
        break;
      default:
        throw new IllegalArgumentException("Invalid type " + dataSourceType);
    }
  }

  private String getAppdynamicsMetricPackDsl(MetricPack metricPack) {
    switch (metricPack.getIdentifier()) {
      case PERFORMANCE_PACK_IDENTIFIER:
        return APPDYNAMICS_PERFORMANCE_PACK_DSL;
      case ERRORS_PACK_IDENTIFIER:
        return APPDYNAMICS_QUALITY_PACK_DSL;
      case INFRASTRUCTURE_PACK_IDENTIFIER:
        return APPDYNAMICS_INFRASTRUCTURE_PACK_DSL;

      default:
        throw new IllegalArgumentException("Invalid identifier " + metricPack.getIdentifier());
    }
  }
}
