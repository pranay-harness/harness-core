package io.harness.cvng.core.services.impl;

import static io.harness.cvng.core.services.CVNextGenConstants.ERRORS_PACK_IDENTIFIER;
import static io.harness.cvng.core.services.CVNextGenConstants.INFRASTRUCTURE_PACK_IDENTIFIER;
import static io.harness.cvng.core.services.CVNextGenConstants.PERFORMANCE_PACK_IDENTIFIER;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.govern.Switch.unhandled;
import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.cvng.beans.CVMonitoringCategory;
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
  // TODO: Automatically read metricPack files:
  static final List<String> APPDYNAMICS_METRICPACK_FILES =
      Lists.newArrayList("/appdynamics/metric-packs/peformance-pack.yml", "/appdynamics/metric-packs/quality-pack.yml");

  static final List<String> NEWRELIC_METRICPACK_FILES =
      Lists.newArrayList("/newrelic/metric-packs/performance-pack.yml");

  static final List<String> STACKDRIVER_METRICPACK_FILES =
      Lists.newArrayList("/stackdriver/metric-packs/default-performance-pack.yaml",
          "/stackdriver/metric-packs/default-error-pack.yaml", "/stackdriver/metric-packs/default-infra-pack.yaml");

  static final List<String> PROMETHEUS_METRICPACK_FILES =
      Lists.newArrayList("/prometheus/metric-packs/default-performance-pack.yaml",
          "/prometheus/metric-packs/default-error-pack.yaml", "/prometheus/metric-packs/default-infra-pack.yaml");

  private static final URL APPDYNAMICS_PERFORMANCE_PACK_DSL_PATH =
      MetricPackServiceImpl.class.getResource("/appdynamics/dsl/performance-pack.datacollection");
  public static final String APPDYNAMICS_PERFORMANCE_PACK_DSL;
  private static final URL APPDYNAMICS_QUALITY_PACK_DSL_PATH =
      MetricPackServiceImpl.class.getResource("/appdynamics/dsl/quality-pack.datacollection");
  public static final String APPDYNAMICS_QUALITY_PACK_DSL;
  private static final URL APPDYNAMICS_INFRASTRUCTURE_PACK_DSL_PATH =
      MetricPackServiceImpl.class.getResource("/appdynamics/dsl/infrastructure-pack.datacollection");
  public static final String APPDYNAMICS_INFRASTRUCTURE_PACK_DSL;

  private static final URL STACKDRIVER_DSL_PATH =
      MetricPackServiceImpl.class.getResource("/stackdriver/dsl/metric-collection.datacollection");
  public static final String STACKDRIVER_DSL;

  private static final URL PROMETHEUS_DSL_PATH =
      MetricPackServiceImpl.class.getResource("/prometheus/dsl/metric-collection.datacollection");
  public static final String PROMETHEUS_DSL;

  private static final URL NEW_RELIC_DSL_PATH =
      MetricPackServiceImpl.class.getResource("/newrelic/dsl/performance-pack.datacollection");
  public static final String NEW_RELIC_DSL;
  static {
    String appDPeformancePackDsl = null;
    String appDqualityPackDsl = null;
    String appDInfrastructurePackDsl = null;
    String stackDriverDsl = null;
    String newrelicDsl = null;
    String prometheusDsl = null;
    try {
      appDPeformancePackDsl = Resources.toString(APPDYNAMICS_PERFORMANCE_PACK_DSL_PATH, Charsets.UTF_8);
      appDqualityPackDsl = Resources.toString(APPDYNAMICS_QUALITY_PACK_DSL_PATH, Charsets.UTF_8);
      appDInfrastructurePackDsl = Resources.toString(APPDYNAMICS_INFRASTRUCTURE_PACK_DSL_PATH, Charsets.UTF_8);
      stackDriverDsl = Resources.toString(STACKDRIVER_DSL_PATH, Charsets.UTF_8);
      newrelicDsl = Resources.toString(NEW_RELIC_DSL_PATH, Charsets.UTF_8);
      prometheusDsl = Resources.toString(PROMETHEUS_DSL_PATH, Charsets.UTF_8);
    } catch (Exception e) {
      // TODO: this should throw an exception but we risk delegate not starting up. We can remove this log term and
      // throw and exception once things stabilize
      log.error("Invalid metric pack dsl path", e);
    }
    APPDYNAMICS_PERFORMANCE_PACK_DSL = appDPeformancePackDsl;
    APPDYNAMICS_QUALITY_PACK_DSL = appDqualityPackDsl;
    APPDYNAMICS_INFRASTRUCTURE_PACK_DSL = appDInfrastructurePackDsl;
    STACKDRIVER_DSL = stackDriverDsl;
    NEW_RELIC_DSL = newrelicDsl;
    PROMETHEUS_DSL = prometheusDsl;
  }

  @Inject private HPersistence hPersistence;

  @Override
  public List<MetricPackDTO> getMetricPacks(
      DataSourceType dataSourceType, String accountId, String orgIdentifier, String projectIdentifier) {
    return getMetricPacks(accountId, orgIdentifier, projectIdentifier, dataSourceType)
        .stream()
        .map(MetricPack::toDTO)
        .collect(Collectors.toList());
  }

  @Override
  public List<MetricPack> getMetricPacks(
      String accountId, String orgIdentifier, String projectIdentifier, DataSourceType dataSourceType) {
    List<MetricPack> metricPacksFromDb = hPersistence.createQuery(MetricPack.class, excludeAuthority)
                                             .filter(MetricPackKeys.accountId, accountId)
                                             .filter(MetricPackKeys.projectIdentifier, projectIdentifier)
                                             .filter(MetricPackKeys.orgIdentifier, orgIdentifier)
                                             .filter(MetricPackKeys.dataSourceType, dataSourceType)
                                             .asList();

    if (isEmpty(metricPacksFromDb)) {
      final Map<String, MetricPack> metricPackDefinitionsFromYaml =
          getMetricPackDefinitionsFromYaml(accountId, orgIdentifier, projectIdentifier, dataSourceType);
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

  @Override
  public MetricPack getMetricPack(String accountId, String orgIdentifier, String projectIdentifier,
      DataSourceType dataSourceType, CVMonitoringCategory cvMonitoringCategory) {
    MetricPack metricPackFromDb = hPersistence.createQuery(MetricPack.class, excludeAuthority)
                                      .filter(MetricPackKeys.accountId, accountId)
                                      .filter(MetricPackKeys.projectIdentifier, projectIdentifier)
                                      .filter(MetricPackKeys.orgIdentifier, orgIdentifier)
                                      .filter(MetricPackKeys.dataSourceType, dataSourceType)
                                      .filter(MetricPackKeys.category, cvMonitoringCategory)
                                      .get();
    Preconditions.checkNotNull(
        metricPackFromDb, String.format("No Metric Packs found for Category %s", cvMonitoringCategory));
    return metricPackFromDb;
  }

  @Override
  public void createDefaultMetricPackAndThresholds(String accountId, String orgIdentifier, String projectIdentifier) {
    List<DataSourceType> dataSourceTypes = DataSourceType.getTimeSeriesTypes();

    for (DataSourceType dataSourceType : dataSourceTypes) {
      final Map<String, MetricPack> metricPackDefinitionsFromYaml =
          getMetricPackDefinitionsFromYaml(accountId, orgIdentifier, projectIdentifier, dataSourceType);
      final ArrayList<MetricPack> metricPacks = Lists.newArrayList(metricPackDefinitionsFromYaml.values());

      if (isEmpty(getMetricPacks(accountId, orgIdentifier, projectIdentifier, dataSourceType))) {
        hPersistence.save(metricPacks);
        metricPacks.forEach(metricPack
            -> createDefaultIgnoreThresholds(
                accountId, orgIdentifier, projectIdentifier, metricPack.getIdentifier(), dataSourceType));
      }
    }
  }

  private Map<String, MetricPack> getMetricPackDefinitionsFromYaml(
      String accountId, String orgIdentifier, String projectIdentifier, DataSourceType dataSourceType) {
    Map<String, MetricPack> metricPacks = new HashMap<>();

    // TODO: when new datasource is on boarded we should stub this out to its own piece so that the switch statements do
    // not grow
    List<String> yamlFileNames = new ArrayList<>();
    switch (dataSourceType) {
      case APP_DYNAMICS:
        yamlFileNames.addAll(APPDYNAMICS_METRICPACK_FILES);
        break;
      case STACKDRIVER:
        yamlFileNames.addAll(STACKDRIVER_METRICPACK_FILES);
        break;
      case PROMETHEUS:
        yamlFileNames.addAll(PROMETHEUS_METRICPACK_FILES);
        break;
      case NEW_RELIC:
        yamlFileNames.addAll(NEWRELIC_METRICPACK_FILES);
        break;
      default:
        unhandled(dataSourceType);
        throw new IllegalStateException("Invalid dataSourceType " + dataSourceType);
    }

    yamlFileNames.forEach(metricPackPath -> {
      try {
        final String metricPackYaml =
            Resources.toString(MetricPackService.class.getResource(metricPackPath), Charsets.UTF_8);
        MetricPack metricPack = buildPack(metricPackYaml, accountId, orgIdentifier, projectIdentifier, dataSourceType);
        metricPacks.put(metricPack.getIdentifier(), metricPack);
      } catch (IOException e) {
        throw new IllegalStateException("Error reading metric packs", e);
      }
    });
    return metricPacks;
  }

  private MetricPack buildPack(String metricPackYaml, String accountId, String orgIdentifier, String projectIdentifier,
      DataSourceType dataSourceType) throws IOException {
    YamlUtils yamlUtils = new YamlUtils();
    final MetricPack metricPack = yamlUtils.read(metricPackYaml, new TypeReference<MetricPack>() {});
    metricPack.setAccountId(accountId);
    metricPack.setOrgIdentifier(orgIdentifier);
    metricPack.setProjectIdentifier(projectIdentifier);
    metricPack.setDataSourceType(dataSourceType);
    metricPack.getMetrics().forEach(metricDefinition -> metricDefinition.setThresholds(Collections.emptyList()));
    return metricPack;
  }

  @Override
  public boolean saveMetricPacks(String accountId, String orgIdentifier, String projectIdentifier,
      DataSourceType dataSourceType, List<MetricPack> metricPacks) {
    if (isEmpty(metricPacks)) {
      return false;
    }

    final Map<String, MetricPack> metricPackDefinitions =
        getMetricPackDefinitionsFromYaml(accountId, orgIdentifier, projectIdentifier, dataSourceType);

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
  public List<TimeSeriesThreshold> getMetricPackThresholds(String accountId, String orgIdentifier,
      String projectIdentifier, String metricPackIdentifier, DataSourceType dataSourceType) {
    List<TimeSeriesThreshold> timeSeriesThresholds =
        hPersistence.createQuery(TimeSeriesThreshold.class, excludeAuthority)
            .filter(TimeSeriesThresholdKeys.accountId, accountId)
            .filter(TimeSeriesThresholdKeys.orgIdentifier, orgIdentifier)
            .filter(TimeSeriesThresholdKeys.projectIdentifier, projectIdentifier)
            .filter(TimeSeriesThresholdKeys.metricPackIdentifier, metricPackIdentifier)
            .filter(TimeSeriesThresholdKeys.dataSourceType, dataSourceType)
            .asList();
    if (isEmpty(timeSeriesThresholds)) {
      return createDefaultIgnoreThresholds(
          accountId, orgIdentifier, projectIdentifier, metricPackIdentifier, dataSourceType);
    }

    return timeSeriesThresholds;
  }

  private List<TimeSeriesThreshold> createDefaultIgnoreThresholds(String accountId, String orgIdentifier,
      String projectIdentifier, String metricPackIdentifier, DataSourceType dataSourceType) {
    MetricPack metricPack = hPersistence.createQuery(MetricPack.class, excludeAuthority)
                                .filter(MetricPackKeys.accountId, accountId)
                                .filter(MetricPackKeys.orgIdentifier, orgIdentifier)
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
                                          .orgIdentifier(orgIdentifier)
                                          .projectIdentifier(projectIdentifier)
                                          .dataSourceType(dataSourceType)
                                          .metricType(metricDefinition.getType())
                                          .metricPackIdentifier(metricPackIdentifier)
                                          .metricName(metricDefinition.getName())
                                          .action(TimeSeriesThresholdActionType.IGNORE)
                                          .criteria(threshold)
                                          .build()));
    });
    saveMetricPackThreshold(accountId, orgIdentifier, projectIdentifier, dataSourceType, timeSeriesThresholds);
    return timeSeriesThresholds;
  }

  @Override
  public List<String> saveMetricPackThreshold(String accountId, String orgIdentifier, String projectIdentifier,
      DataSourceType dataSourceType, List<TimeSeriesThreshold> timeSeriesThresholds) {
    timeSeriesThresholds.forEach(timeSeriesThreshold -> {
      timeSeriesThreshold.setAccountId(accountId);
      timeSeriesThreshold.setOrgIdentifier(orgIdentifier);
      timeSeriesThreshold.setProjectIdentifier(projectIdentifier);
      timeSeriesThreshold.setDataSourceType(dataSourceType);
    });
    return hPersistence.save(timeSeriesThresholds);
  }

  @Override
  public boolean deleteMetricPackThresholds(
      String accountId, String orgIdentifier, String projectIdentifier, String thresholdId) {
    return hPersistence.delete(TimeSeriesThreshold.class, thresholdId);
  }

  @Override
  public void populatePaths(String accountId, String orgIdentifier, String projectIdentifier,
      DataSourceType dataSourceType, MetricPack metricPack) {
    final List<MetricPack> metricPacksForProject =
        getMetricPacks(accountId, orgIdentifier, projectIdentifier, dataSourceType);
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
      case STACKDRIVER:
        metricPack.setDataCollectionDsl(STACKDRIVER_DSL);
        break;
      case PROMETHEUS:
        metricPack.setDataCollectionDsl(PROMETHEUS_DSL);
        break;
      case NEW_RELIC:
        metricPack.setDataCollectionDsl(NEW_RELIC_DSL);
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
