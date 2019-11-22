package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.common.VerificationConstants.DD_ECS_HOST_NAME;
import static software.wings.common.VerificationConstants.DD_HOST_NAME_EXPRESSION;
import static software.wings.common.VerificationConstants.DD_K8s_HOST_NAME;
import static software.wings.metrics.MetricType.ERROR;
import static software.wings.metrics.MetricType.RESP_TIME;
import static software.wings.metrics.MetricType.THROUGHPUT;
import static software.wings.utils.Misc.replaceDotWithUnicode;

import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.DelegateTask;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.TaskData;
import io.harness.exception.WingsException;
import io.harness.serializer.JsonUtils;
import io.harness.serializer.YamlUtils;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import software.wings.api.DeploymentType;
import software.wings.beans.DatadogConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.TemplateExpression;
import software.wings.metrics.MetricType;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisComparisonStrategyProvider;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.AnalysisToleranceProvider;
import software.wings.service.impl.analysis.DataCollectionCallback;
import software.wings.service.impl.analysis.TimeSeriesMetricGroup;
import software.wings.service.impl.analysis.TimeSeriesMlAnalysisType;
import software.wings.service.impl.apm.APMDataCollectionInfo;
import software.wings.service.impl.apm.APMMetricInfo;
import software.wings.service.impl.apm.APMMetricInfo.APMMetricInfoBuilder;
import software.wings.service.impl.apm.APMMetricInfo.ResponseMapper;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.verification.VerificationStateAnalysisExecutionData;
import software.wings.verification.datadog.DatadogCVServiceConfiguration;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
@Slf4j
public class DatadogState extends AbstractMetricAnalysisState {
  private static final int DATA_COLLECTION_RATE_MINS = 5;
  private static final URL DATADOG_URL = DatadogState.class.getResource("/apm/datadog.yml");
  private static final URL DATADOG_METRICS_URL = DatadogState.class.getResource("/apm/datadog_metrics.yml");
  private static final String DATADOG_METRICS_YAML, DATADOG_YAML;
  static {
    String tmpDatadogMetricsYaml = "", tmpDatadogYaml = "";
    try {
      tmpDatadogMetricsYaml = Resources.toString(DATADOG_METRICS_URL, Charsets.UTF_8);
      tmpDatadogYaml = Resources.toString(DATADOG_URL, Charsets.UTF_8);
    } catch (IOException ex) {
      logger.error("Unable to initialize datadog metrics yaml");
    }
    DATADOG_METRICS_YAML = tmpDatadogMetricsYaml;
    DATADOG_YAML = tmpDatadogYaml;
  }

  public DatadogState(String name) {
    super(name, StateType.DATA_DOG);
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Attributes(required = true, title = "Datadog Server") private String analysisServerConfigId;

  @Attributes(required = false, title = "Datadog Service Name") private String datadogServiceName;

  @Attributes(required = false, title = "Metrics") private String metrics;

  @Attributes(required = false, title = "Custom Metrics") private Map<String, Set<Metric>> customMetrics;

  @EnumData(enumDataProvider = AnalysisToleranceProvider.class)
  @Attributes(required = true, title = "Algorithm Sensitivity")
  @DefaultValue("MEDIUM")
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
  public Map<String, String> validateFields() {
    Map<String, String> validateFields = new HashMap<>();
    if (isNotEmpty(customMetrics)) {
      validateFields.putAll(validateDatadogCustomMetrics(customMetrics));
    }
    if (isNotEmpty(metrics)) {
      List<String> metricList = Arrays.asList(metrics.split(","));
      metricList.forEach(metric -> {
        if (metric.startsWith("trace.")) {
          validateFields.put(metric, "Unsupported metric type for workflow verification");
        }
      });
    }
    return validateFields;
  }

  @Override
  protected String triggerAnalysisDataCollection(ExecutionContext context, AnalysisContext analysisContext,
      VerificationStateAnalysisExecutionData executionData, Map<String, String> hosts) {
    List<String> metricNames = metrics != null ? Arrays.asList(metrics.split(",")) : Collections.EMPTY_LIST;
    String hostFilter = getDeploymentType(context).equals(DeploymentType.ECS) ? DD_ECS_HOST_NAME : DD_K8s_HOST_NAME;
    metricAnalysisService.saveMetricTemplates(context.getAppId(), StateType.DATA_DOG,
        context.getStateExecutionInstanceId(), null,
        metricDefinitions(metrics(Optional.of(metricNames), Optional.ofNullable(datadogServiceName),
            Optional.ofNullable(customMetrics), Optional.empty(), Optional.of(hostFilter))
                              .values()));

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
    final long dataCollectionStartTimeStamp = dataCollectionStartTimestampMillis();
    String accountId = appService.get(context.getAppId()).getAccountId();
    int timeDurationInInteger = Integer.parseInt(getTimeDuration());
    final APMDataCollectionInfo dataCollectionInfo =
        APMDataCollectionInfo.builder()
            .baseUrl(datadogConfig.getUrl())
            .validationUrl(DatadogConfig.validationUrl)
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
            .metricEndpoints(metricEndpointsInfo(Optional.ofNullable(serviceName), Optional.of(metricNames),
                Optional.empty(), Optional.ofNullable(customMetrics), Optional.ofNullable(getDeploymentType(context))))
            .accountId(accountId)
            .strategy(getComparisonStrategy())
            .dataCollectionFrequency(DATA_COLLECTION_RATE_MINS)
            .dataCollectionTotalTime(timeDurationInInteger)
            .initialDelaySeconds(getDelaySeconds(initialAnalysisDelay))
            .build();

    String waitId = generateUuid();
    String infrastructureMappingId = context.fetchInfraMappingId();
    DelegateTask delegateTask = DelegateTask.builder()
                                    .async(true)
                                    .accountId(accountId)
                                    .appId(context.getAppId())
                                    .waitId(waitId)
                                    .data(TaskData.builder()
                                              .taskType(TaskType.APM_METRIC_DATA_COLLECTION_TASK.name())
                                              .parameters(new Object[] {dataCollectionInfo})
                                              .timeout(TimeUnit.MINUTES.toMillis(timeDurationInInteger + 120))
                                              .build())
                                    .envId(envId)
                                    .infrastructureMappingId(infrastructureMappingId)
                                    .build();
    waitNotifyEngine.waitForAll(
        DataCollectionCallback.builder().appId(context.getAppId()).executionData(executionData).build(), waitId);

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

  public Map<String, Set<Metric>> fetchCustomMetrics() {
    return customMetrics;
  }

  public void setCustomMetrics(Map<String, Set<Metric>> customMetrics) {
    this.customMetrics = customMetrics;
  }

  public static String getMetricTypeForMetric(String metricName, DatadogCVServiceConfiguration cvConfig) {
    try {
      YamlUtils yamlUtils = new YamlUtils();

      Map<String, List<Metric>> metricsMap =
          yamlUtils.read(DATADOG_METRICS_YAML, new TypeReference<Map<String, List<Metric>>>() {});
      List<Metric> metrics = metricsMap.values().stream().flatMap(List::stream).collect(Collectors.toList());
      Optional<Metric> matchedMetric =
          metrics.stream().filter(metric -> metric.getMetricName().equals(metricName)).findAny();
      if (matchedMetric.isPresent()) {
        return matchedMetric.get().getMlMetricType();
      }
      if (cvConfig != null && isNotEmpty(cvConfig.getCustomMetrics())) {
        for (Entry<String, Set<Metric>> customMetricEntry : cvConfig.getCustomMetrics().entrySet()) {
          if (isNotEmpty(customMetricEntry.getValue())) {
            for (Metric metric : customMetricEntry.getValue()) {
              if (metricName.equals(metric.getDisplayName())) {
                return metric.getMlMetricType();
              }
            }
          }
        }
      }
    } catch (Exception e) {
      logger.error("Exception occurred while calculating metric type for name: {}", metricName, e);
    }
    return null;
  }

  public static Map<String, List<APMMetricInfo>> metricEndpointsInfo(Optional<String> datadogServiceName,
      Optional<List<String>> metricNames, Optional<String> applicationFilter,
      Optional<Map<String, Set<Metric>>> customMetrics, Optional<DeploymentType> deploymentType) {
    YamlUtils yamlUtils = new YamlUtils();

    try {
      Map<String, MetricInfo> metricInfos =
          yamlUtils.read(DATADOG_YAML, new TypeReference<Map<String, MetricInfo>>() {});

      if (!metricNames.isPresent()) {
        metricNames = Optional.of(new ArrayList<>());
      }

      String hostFilter;
      if (deploymentType.isPresent() && deploymentType.get().equals(DeploymentType.ECS)) {
        parseMetricInfo(metricInfos.get("Docker"), DD_ECS_HOST_NAME);
        hostFilter = DD_ECS_HOST_NAME;
      } else {
        parseMetricInfo(metricInfos.get("Docker"), DD_K8s_HOST_NAME);
        hostFilter = DD_K8s_HOST_NAME;
      }
      Map<String, Metric> metricMap =
          metrics(metricNames, datadogServiceName, customMetrics, applicationFilter, Optional.of(hostFilter));

      // metrics list will have list of metric objects for given MetricNames
      List<Metric> metrics = new ArrayList<>();
      for (String metricName : metricNames.get()) {
        if (!metricMap.containsKey(metricName)) {
          throw new WingsException("metric name not found" + metricName);
        }
        metrics.add(metricMap.get(metricName));
      }

      Map<String, List<APMMetricInfo>> result = new HashMap<>();
      for (Metric metric : metrics) {
        APMMetricInfoBuilder newMetricInfoBuilder = APMMetricInfo.builder();
        MetricInfo metricInfo = metricInfos.get(metric.getDatadogMetricType());

        String metricUrl = getMetricURL(metricInfo, metric.getDatadogMetricType(), deploymentType);
        newMetricInfoBuilder.responseMappers(metricInfo.responseMapperMap());
        newMetricInfoBuilder.metricType(MetricType.valueOf(metric.getMlMetricType()));
        newMetricInfoBuilder.tag(metric.getDatadogMetricType());
        newMetricInfoBuilder.responseMappers(metricInfo.responseMapperMap());
        newMetricInfoBuilder.metricName(metric.getDisplayName());

        if (Arrays.asList("System", "Kubernetes", "Docker", "ECS").contains(metric.getDatadogMetricType())) {
          metricUrl = metricUrl.replace("${query}", metric.getMetricName());
          if (applicationFilter.isPresent()) {
            metricUrl = metricUrl.replace("${applicationFilter}", applicationFilter.get());
          }

          metricUrl = parseTransformationUnit(metricUrl, deploymentType, metric);

          if (!result.containsKey(metricUrl)) {
            result.put(metricUrl, new ArrayList<>());
          }
          result.get(metricUrl).add(newMetricInfoBuilder.build());
        } else if (metric.getDatadogMetricType().equals("Servlet")) {
          if (datadogServiceName.isPresent()) {
            metricUrl = metricUrl.replace("${datadogServiceName}", datadogServiceName.get());
          }
          metricUrl = metricUrl.replace("${query}", metric.getMetricName());

          if (!applicationFilter.isPresent()) {
            applicationFilter = Optional.of("");
          }
          metricUrl = metricUrl.replace("${applicationFilter}", applicationFilter.get());

          if (!result.containsKey(metricUrl)) {
            result.put(metricUrl, new ArrayList<>());
          }
          result.get(metricUrl).add(newMetricInfoBuilder.build());
        } else {
          throw new WingsException("Unsupported template type for" + metric);
        }
      }

      if (customMetrics.isPresent()) {
        for (String identifier : customMetrics.get().keySet()) {
          // identifier can be host_identifier or application filter
          if (deploymentType.isPresent()) {
            parseMetricInfo(metricInfos.get("Custom"), identifier);
          } else {
            parseMetricInfo(metricInfos.get("Custom"), DD_K8s_HOST_NAME);
          }
          Set<Metric> metricSet = customMetrics.get().get(identifier);
          metricSet.forEach(metric -> {
            MetricInfo metricInfo = metricInfos.get(metric.getDatadogMetricType());

            APMMetricInfoBuilder newMetricInfoBuilder = APMMetricInfo.builder();
            // update the response mapper with the transaction/group name.
            Map<String, ResponseMapper> responseMapperMap = metricInfo.responseMapperMap();
            String txnName = "Transaction Group 1";
            if (isNotEmpty(metric.getTxnName())) {
              txnName = metric.getTxnName();
            }
            responseMapperMap.put("txnName", ResponseMapper.builder().fieldName("txnName").fieldValue(txnName).build());
            newMetricInfoBuilder.responseMappers(responseMapperMap);
            newMetricInfoBuilder.metricType(MetricType.valueOf(metric.getMlMetricType()));
            newMetricInfoBuilder.tag(metric.getDatadogMetricType());
            newMetricInfoBuilder.metricName(metric.getDisplayName());

            String metricUrl = getMetricURL(metricInfo, metric.getDatadogMetricType(), deploymentType);
            metricUrl = metricUrl.replace("${query}", metric.getMetricName());
            if (deploymentType.isPresent()) {
              metricUrl = metricUrl.replace("${host_identifier}", identifier);
            } else {
              metricUrl = metricUrl.replace("${applicationFilter}", identifier);
            }
            if (!result.containsKey(metricUrl)) {
              result.put(metricUrl, new ArrayList<>());
            }
            result.get(metricUrl).add(newMetricInfoBuilder.build());
          });
        }
      }
      return result;
    } catch (RuntimeException | IOException ex) {
      throw new WingsException("Unable to get metric info", ex);
    }
  }

  private static String parseTransformationUnit(
      String metricUrl, Optional<DeploymentType> deploymentType, Metric metric) {
    if (deploymentType.isPresent()) {
      // workflow based deployment
      if (isEmpty(metric.getTransformation())) {
        metricUrl = metricUrl.replace("${transformUnits}", "");
      } else {
        metricUrl = metricUrl.replace("${transformUnits}", metric.getTransformation());
      }
    } else {
      if (isEmpty(metric.getTransformation24x7())) {
        metricUrl = metricUrl.replace("${transformUnits}", "");
      } else {
        metricUrl = metricUrl.replace("${transformUnits}", metric.getTransformation24x7());
      }
    }
    return metricUrl;
  }

  private static String getMetricURL(
      MetricInfo metricInfo, String datadogMetricType, Optional<DeploymentType> deploymentType) {
    String metricUrl;
    if (deploymentType.isPresent()) {
      metricUrl = deploymentType.get().equals(DeploymentType.ECS) && datadogMetricType.equals("Docker")
          ? metricInfo.getUrlEcs()
          : metricInfo.getUrl();
    } else {
      metricUrl = metricInfo.getUrl24x7();
    }
    return metricUrl;
  }

  private static void parseMetricInfo(MetricInfo metricInfo, String hostname) {
    for (ResponseMapper responseMapper : metricInfo.getResponseMappers()) {
      if (responseMapper.getFieldName().equals("host")) {
        responseMapper.getRegexs().replaceAll(regex -> regex.replace(DD_HOST_NAME_EXPRESSION, hostname));
      }
    }
  }

  public static Map<String, TimeSeriesMetricDefinition> metricDefinitions(Collection<Metric> metrics) {
    Map<String, TimeSeriesMetricDefinition> metricTypeMap = new HashMap<>();
    for (Metric metric : metrics) {
      metricTypeMap.put(replaceDotWithUnicode(metric.getDisplayName()),
          TimeSeriesMetricDefinition.builder()
              .metricName(metric.getDisplayName())
              .metricType(MetricType.valueOf(metric.getMlMetricType()))
              .tags(metric.getTags())
              .build());
    }
    return metricTypeMap;
  }

  public static Map<String, Metric> metrics(Optional<List<String>> metricNames, Optional<String> datadogServiceName,
      Optional<Map<String, Set<Metric>>> customMetricsByTag, Optional<String> applicationFilter,
      Optional<String> hostFilter) {
    YamlUtils yamlUtils = new YamlUtils();
    try {
      Map<String, List<Metric>> metrics =
          yamlUtils.read(DATADOG_METRICS_YAML, new TypeReference<Map<String, List<Metric>>>() {});

      if (!metricNames.isPresent()) {
        metricNames = Optional.of(new ArrayList<>());
      }
      // if datadog service name provided then analysis will be done for servlet metrics
      if (datadogServiceName.isPresent()) {
        // add the servlet metrics to this list.
        List<String> servletMetrics = new ArrayList<>();
        metrics.get("Servlet").forEach(servletMetric -> { servletMetrics.add(servletMetric.getMetricName()); });
        metricNames.get().addAll(servletMetrics);
      }

      Map<String, Metric> metricMap = new HashMap<>();
      Set<String> metricNamesSet = Sets.newHashSet(metricNames.get());

      // add servlet, docker, ecs metrics to the map
      for (Map.Entry<String, List<Metric>> entry : metrics.entrySet()) {
        entry.getValue().forEach(metric -> {
          if (metricNamesSet.contains(metric.getMetricName())) {
            if (metric.getTags() == null) {
              metric.setTags(new HashSet());
            }
            metric.getTags().add(entry.getKey());

            // transformation24x7 needs to use application filter in transformation metric as well.
            if (applicationFilter.isPresent() && isNotEmpty(metric.getTransformation24x7())) {
              metric.setTransformation24x7(
                  metric.getTransformation24x7().replace("${applicationFilter}", applicationFilter.get()));
            }
            if (hostFilter.isPresent() && metric.getDatadogMetricType().equals("Docker")
                && isNotEmpty(metric.getTransformation())) {
              metric.setTransformation(metric.getTransformation().replace("${hostFilter}", hostFilter.get()));
            }

            metricMap.put(metric.getMetricName(), metric);
          }
        });
      }

      // add custom metrics to the map
      if (customMetricsByTag.isPresent()) {
        for (Entry<String, Set<Metric>> entry : customMetricsByTag.get().entrySet()) {
          entry.getValue().forEach(metric -> {
            metric.setTags(new HashSet<>());
            metric.getTags().add("Custom");
            metricMap.put(metric.getMetricName(), metric);
          });
        }
      }
      return metricMap;
    } catch (Exception ex) {
      throw new WingsException("Unable to load datadog metrics", ex);
    }
  }

  public static List<Metric> metricNames() {
    YamlUtils yamlUtils = new YamlUtils();
    try {
      Map<String, List<Metric>> metricsMap =
          yamlUtils.read(DATADOG_METRICS_YAML, new TypeReference<Map<String, List<Metric>>>() {});
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

  /**
   * Validate the fields for custom metrics - for each txn, there should be only one throughput
   * If error/response time is present, throughput should be present too.
   * If only throughput is present, we won't analyze it.
   * @param customMetrics
   * @return
   */
  public static Map<String, String> validateDatadogCustomMetrics(Map<String, Set<Metric>> customMetrics) {
    if (isNotEmpty(customMetrics)) {
      Map<String, String> invalidFields = new HashMap<>();
      // group the metrics by txn.
      Map<String, Set<Metric>> txnMetricMap = new HashMap<>();
      customMetrics.forEach((filter, metricSet) -> {
        List<Metric> metricList = new ArrayList<>();
        for (Object metricObj : metricSet) {
          Metric metric = JsonUtils.asObject(JsonUtils.asJson(metricObj), Metric.class);
          metricList.add(metric);
        }
        metricList.forEach(metric -> {
          String txnFilter = filter + "-" + metric.getTxnName();
          if (!txnMetricMap.containsKey(txnFilter)) {
            txnMetricMap.put(txnFilter, new HashSet<>());
          }
          txnMetricMap.get(txnFilter).add(metric);
        });
      });

      // validate the txnMetricMap for the ones mentioned above.
      txnMetricMap.forEach((txnName, metricSet) -> {
        AtomicInteger throughputCount = new AtomicInteger(0);
        AtomicInteger otherMetricsCount = new AtomicInteger(0);
        AtomicInteger errorResponseCount = new AtomicInteger(0);
        metricSet.forEach(metric -> {
          if (metric.getMlMetricType().equals(THROUGHPUT.name())) {
            throughputCount.incrementAndGet();
          } else {
            otherMetricsCount.incrementAndGet();
            if (metric.getMlMetricType().equals(RESP_TIME.name()) || metric.getMlMetricType().equals(ERROR.name())) {
              errorResponseCount.incrementAndGet();
            }
          }
        });

        if (throughputCount.get() > 1) {
          invalidFields.put("Incorrect throughput configuration for group: " + txnName,
              "There are more than one throughput metrics defined.");
        }
        if (otherMetricsCount.get() == 0 && throughputCount.get() != 0) {
          invalidFields.put("Invalid metric configuration for group: " + txnName,
              "It has only throughput metrics. Throughput metrics is used to analyze other metrics and is not analyzed.");
        }
        if (errorResponseCount.get() > 0 && throughputCount.get() == 0) {
          invalidFields.put("Incorrect configuration for group: " + txnName,
              "Error or Response metrics have been defined for " + txnName
                  + " but there is no definition for a throughput metric.");
        }
      });

      return invalidFields;
    }

    return null;
  }

  @Data
  @Builder
  public static class Metric {
    private String metricName;
    private String mlMetricType;
    private String datadogMetricType;
    private String displayName;
    private String transformation;
    private String transformation24x7;
    private Set<String> tags;
    private String txnName; // this field is optional. It can be extracted from the response
  }

  @Data
  @Builder
  public static class MetricInfo {
    private String url;
    private String urlEcs;
    private String url24x7;
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
