package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;

import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.data.structure.EmptyPredicate;
import io.harness.serializer.YamlUtils;
import io.harness.time.Timestamp;
import lombok.Builder;
import lombok.Data;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.PhaseElement;
import software.wings.beans.DatadogConfig;
import software.wings.beans.DelegateTask;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.TemplateExpression;
import software.wings.common.Constants;
import software.wings.exception.WingsException;
import software.wings.metrics.MetricType;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisComparisonStrategyProvider;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.AnalysisToleranceProvider;
import software.wings.service.impl.analysis.DataCollectionCallback;
import software.wings.service.impl.analysis.TimeSeriesMetricGroup;
import software.wings.service.impl.analysis.TimeSeriesMlAnalysisType;
import software.wings.service.impl.apm.APMDataCollectionInfo;
import software.wings.service.impl.apm.APMMetricInfo;
import software.wings.service.impl.apm.APMMetricInfo.APMMetricInfoBuilder;
import software.wings.service.impl.datadog.DatadogSettingProvider;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class

    DatadogState extends AbstractMetricAnalysisState {
  @Transient @SchemaIgnore private static final Logger logger = LoggerFactory.getLogger(DatadogState.class);
  private static final int DATA_COLLECTION_RATE_MINS = 5;

  public DatadogState(String name) {
    super(name, StateType.DATA_DOG);
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  @EnumData(enumDataProvider = DatadogSettingProvider.class)
  @Attributes(required = true, title = "Datadog Server")
  private String analysisServerConfigId;

  @Attributes(required = false, title = "Datadog Service Name") private String datadogServiceName;

  @Attributes(required = true, title = "Metrics") private String metrics;

  @EnumData(enumDataProvider = AnalysisToleranceProvider.class)
  @Attributes(required = true, title = "Failure Criteria")
  @DefaultValue("LOW")
  public AnalysisTolerance getAnalysisTolerance() {
    if (isBlank(tolerance)) {
      return AnalysisTolerance.LOW;
    }
    return AnalysisTolerance.valueOf(tolerance);
  }

  public static Map<String, TimeSeriesMetricGroup.TimeSeriesMlAnalysisGroupInfo> metricGroup(
      Map<String, List<APMMetricInfo>> metricInfos) {
    Set<String> groups = new HashSet<>();
    for (List<APMMetricInfo> metricInfoList : metricInfos.values()) {
      for (APMMetricInfo metricInfo : metricInfoList) {
        groups.add(metricInfo.getTag());
      }
    }
    Map<String, TimeSeriesMetricGroup.TimeSeriesMlAnalysisGroupInfo> groupInfoMap = new HashMap<>();
    for (String group : groups) {
      groupInfoMap.put(group,
          TimeSeriesMetricGroup.TimeSeriesMlAnalysisGroupInfo.builder()
              .groupName(group)
              .mlAnalysisType(TimeSeriesMlAnalysisType.COMPARATIVE)
              .build());
    }
    if (groupInfoMap.size() == 0) {
      throw new WingsException("No Metric Group Names found. This is a required field");
    }
    return groupInfoMap;
  }

  @Override
  protected String triggerAnalysisDataCollection(ExecutionContext context, String correlationId, Set<String> hosts) {
    List<String> metricNames = Arrays.asList(metrics.split(","));
    metricAnalysisService.saveMetricTemplates(
        StateType.DATA_DOG, context.getStateExecutionInstanceId(), metricDefinitions(metrics(metricNames).values()));
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String envId = workflowStandardParams == null ? null : workflowStandardParams.getEnv().getUuid();
    SettingAttribute settingAttribute = null;
    String serverConfigId = analysisServerConfigId;
    String serviceName = this.datadogServiceName;
    if (!isEmpty(getTemplateExpressions())) {
      TemplateExpression configIdExpression =
          templateExpressionProcessor.getTemplateExpression(getTemplateExpressions(), "analysisServerConfigId");
      if (configIdExpression != null) {
        settingAttribute = templateExpressionProcessor.resolveSettingAttribute(context, configIdExpression);
        serverConfigId = settingAttribute.getUuid();
      }
      TemplateExpression serviceNameExpression =
          templateExpressionProcessor.getTemplateExpression(getTemplateExpressions(), "datadogServiceName");
      if (serviceNameExpression != null) {
        serviceName = templateExpressionProcessor.resolveTemplateExpression(context, serviceNameExpression);
      }
    }
    if (settingAttribute == null) {
      settingAttribute = settingsService.get(serverConfigId);
      if (settingAttribute == null) {
        throw new WingsException("No Datadog setting with id: " + analysisServerConfigId + " found");
      }
    }

    final DatadogConfig datadogConfig = (DatadogConfig) settingAttribute.getValue();
    final long dataCollectionStartTimeStamp = Timestamp.minuteBoundary(System.currentTimeMillis());
    String accountId = appService.get(context.getAppId()).getAccountId();
    int timeDurationInInteger = Integer.parseInt(timeDuration);
    final APMDataCollectionInfo dataCollectionInfo =
        APMDataCollectionInfo.builder()
            .baseUrl(datadogConfig.getUrl())
            .encryptedDataDetails(
                secretManager.getEncryptionDetails(datadogConfig, context.getAppId(), context.getWorkflowExecutionId()))
            .hosts(hosts)
            .stateType(StateType.DATA_DOG)
            .applicationId(context.getAppId())
            .stateExecutionId(context.getStateExecutionInstanceId())
            .workflowId(getWorkflowId(context))
            .workflowExecutionId(context.getWorkflowExecutionId())
            .serviceId(getPhaseServiceId(context))
            .startTime(dataCollectionStartTimeStamp)
            .dataCollectionMinute(0)
            .metricEndpoints(metricEndpointsInfo(serviceName, metricNames))
            .accountId(accountId)
            .strategy(getComparisonStrategy())
            .dataCollectionFrequency(DATA_COLLECTION_RATE_MINS)
            .dataCollectionTotalTime(timeDurationInInteger)
            .build();

    String waitId = generateUuid();
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String infrastructureMappingId = phaseElement == null ? null : phaseElement.getInfraMappingId();
    DelegateTask delegateTask = aDelegateTask()
                                    .withTaskType(TaskType.APM_METRIC_DATA_COLLECTION_TASK)
                                    .withAccountId(accountId)
                                    .withAppId(context.getAppId())
                                    .withWaitId(waitId)
                                    .withParameters(new Object[] {dataCollectionInfo})
                                    .withEnvId(envId)
                                    .withInfrastructureMappingId(infrastructureMappingId)
                                    .withTimeout(TimeUnit.MINUTES.toMillis(timeDurationInInteger + 120))
                                    .build();
    waitNotifyEngine.waitForAll(new DataCollectionCallback(context.getAppId(), correlationId, false), waitId);

    return delegateService.queueTask(delegateTask);
  }

  @EnumData(enumDataProvider = AnalysisComparisonStrategyProvider.class)
  @Attributes(required = true, title = "Baseline for Risk Analysis")
  @DefaultValue("COMPARE_WITH_PREVIOUS")
  public AnalysisComparisonStrategy getComparisonStrategy() {
    if (isBlank(comparisonStrategy)) {
      return AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS;
    }
    return AnalysisComparisonStrategy.valueOf(comparisonStrategy);
  }

  @Attributes(title = "Expression for Host/Container name")
  @DefaultValue("")
  public String getHostnameTemplate() {
    return hostnameTemplate;
  }

  public void setHostnameTemplate(String hostnameTemplate) {
    this.hostnameTemplate = hostnameTemplate;
  }

  @SchemaIgnore
  protected String getStateBaseUrl() {
    return "datadog";
  }

  @Override
  public String getAnalysisServerConfigId() {
    return analysisServerConfigId;
  }

  @Override
  public void setAnalysisServerConfigId(String analysisServerConfigId) {
    this.analysisServerConfigId = analysisServerConfigId;
  }

  public String getDatadogServiceName() {
    return datadogServiceName;
  }

  public void setDatadogServiceName(String datadogServiceName) {
    this.datadogServiceName = datadogServiceName;
  }

  @SuppressFBWarnings("REC_CATCH_EXCEPTION")
  public static Map<String, List<APMMetricInfo>> metricEndpointsInfo(String serviceName, List<String> metricNames) {
    YamlUtils yamlUtils = new YamlUtils();
    URL url = DatadogState.class.getResource("/apm/datadog.yml");
    try {
      String yaml = Resources.toString(url, Charsets.UTF_8);
      Map<String, MetricInfo> metricInfos = yamlUtils.read(yaml, new TypeReference<Map<String, MetricInfo>>() {});
      Map<String, Metric> metricMap = metrics(metricNames);
      List<Metric> metrics = new ArrayList<>();
      for (String metricName : metricNames) {
        if (!metricMap.containsKey(metricName)) {
          throw new WingsException("metric name not found" + metricName);
        }
        metrics.add(metricMap.get(metricName));
      }
      Map<String, List<APMMetricInfo>> result = new HashMap<>();
      for (Metric metric : metrics) {
        APMMetricInfoBuilder newMetricInfoBuilder = APMMetricInfo.builder();
        MetricInfo metricInfo = metricInfos.get(metric.getDatadogMetricType());
        String metricUrl = metricInfo.getUrl();
        newMetricInfoBuilder.responseMappers(metricInfo.responseMapperMap());
        newMetricInfoBuilder.metricName(metric.getDisplayName());
        newMetricInfoBuilder.metricType(metric.getMlMetricType());
        newMetricInfoBuilder.tag(metric.getDatadogMetricType());
        newMetricInfoBuilder.responseMappers(metricInfo.responseMapperMap());
        newMetricInfoBuilder.metricName(metric.getDisplayName());

        if (Arrays.asList("System", "Kubernetes", "Docker").contains(metric.getDatadogMetricType())) {
          metricUrl = metricUrl.replace("${query}", metric.getMetricName());
          if (EmptyPredicate.isEmpty(metric.getTransformation())) {
            metricUrl = metricUrl.replace("${transformUnits}", "");
          } else {
            metricUrl = metricUrl.replace("${transformUnits}", metric.getTransformation());
          }

          if (!result.containsKey(metricUrl)) {
            result.put(metricUrl, new ArrayList<>());
          }
          result.get(metricUrl).add(newMetricInfoBuilder.build());
        } else if (metric.getDatadogMetricType().equals("Servlet")) {
          metricUrl =
              metricUrl.replace("${datadogServiceName}", serviceName).replace("${query}", metric.getMetricName());
          if (!result.containsKey(metricUrl)) {
            result.put(metricUrl, new ArrayList<>());
          }
          result.get(metricUrl).add(newMetricInfoBuilder.build());
        } else {
          throw new WingsException("Unsupported template type for" + metric);
        }
      }
      return result;
    } catch (Exception ex) {
      throw new WingsException("Unable to get metric info", ex);
    }
  }

  public static Map<String, TimeSeriesMetricDefinition> metricDefinitions(Collection<Metric> metrics) {
    Map<String, TimeSeriesMetricDefinition> metricTypeMap = new HashMap<>();
    for (Metric metric : metrics) {
      metricTypeMap.put(metric.getDisplayName(),
          TimeSeriesMetricDefinition.builder()
              .metricName(metric.getDisplayName())
              .metricType(metric.getMlMetricType())
              .tags(metric.getTags())
              .build());
    }
    return metricTypeMap;
  }

  public static Map<String, Metric> metrics(List<String> metricNames) {
    YamlUtils yamlUtils = new YamlUtils();
    URL url = DatadogState.class.getResource("/apm/datadog_metrics.yml");
    try {
      String yaml = Resources.toString(url, Charsets.UTF_8);
      Map<String, List<Metric>> metrics = yamlUtils.read(yaml, new TypeReference<Map<String, List<Metric>>>() {});
      Map<String, Metric> metricMap = new HashMap<>();
      Set<String> metricNamesSet = Sets.newHashSet(metricNames);
      for (Map.Entry<String, List<Metric>> entry : metrics.entrySet()) {
        entry.getValue().forEach(metric -> {
          if (metricNamesSet.contains(metric.getMetricName())) {
            if (metric.getTags() == null) {
              metric.setTags(new HashSet());
            }
            metric.getTags().add(entry.getKey());
            metricMap.put(metric.getMetricName(), metric);
          }
        });
      }
      return metricMap;
    } catch (Exception ex) {
      throw new WingsException("Unable to load datadog metrics", ex);
    }
  }

  public static List<Metric> metricNames() {
    YamlUtils yamlUtils = new YamlUtils();
    URL url = DatadogState.class.getResource("/apm/datadog_metrics.yml");
    try {
      String yaml = Resources.toString(url, Charsets.UTF_8);
      Map<String, List<Metric>> metricsMap = yamlUtils.read(yaml, new TypeReference<Map<String, List<Metric>>>() {});
      return metricsMap.values().stream().flatMap(metric -> metric.stream()).collect(Collectors.toList());
    } catch (Exception ex) {
      throw new WingsException("Unable to load datadog metrics", ex);
    }
  }

  public String getMetrics() {
    return metrics;
  }

  public void setMetrics(String metrics) {
    this.metrics = metrics;
  }

  @Data
  @Builder
  public static class Metric {
    private String metricName;
    private MetricType mlMetricType;
    private String datadogMetricType;
    private String displayName;
    private String transformation;
    private Set<String> tags;
  }

  @Data
  @Builder
  public static class MetricInfo {
    private String url;
    private List<APMMetricInfo.ResponseMapper> responseMappers;
    public Map<String, APMMetricInfo.ResponseMapper> responseMapperMap() {
      Map<String, APMMetricInfo.ResponseMapper> result = new HashMap<>();
      for (APMMetricInfo.ResponseMapper responseMapper : responseMappers) {
        result.put(responseMapper.getFieldName(), responseMapper);
      }
      return result;
    }
  }
}
