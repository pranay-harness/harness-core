package software.wings.verification;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.exception.HarnessException;

import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.analysis.TimeSeriesMLTransactionThresholds;
import software.wings.service.impl.analysis.TimeSeriesMLTransactionThresholds.TimeSeriesMLTransactionThresholdsDTO;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.intfc.MetricDataAnalysisService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class MetricCVConfigurationYamlHandler<Y extends MetricCVConfigurationYaml, B extends CVConfiguration>
    extends CVConfigurationYamlHandler<Y, B> {
  private static final String CUSTOM_THRESHOLD_KEY = "customThresholdRefId";
  private static final String THRESHOLDS_KEY = "metric-thresholds";
  @Inject private MetricDataAnalysisService metricDataAnalysisService;

  public void toYaml(MetricCVConfigurationYaml yaml, CVConfiguration bean) {
    super.toYaml(yaml, bean);
    if (bean.getCustomThresholdRefId() != null) {
      yaml.setCustomThresholdRefId(bean.getCustomThresholdRefId());
      List<TimeSeriesMLTransactionThresholds> thresholds =
          metricDataAnalysisService.getCustomThreshold(bean.getCustomThresholdRefId());
      List<TimeSeriesMLTransactionThresholdsDTO> thresholdsDTOList = new ArrayList<>();
      if (isNotEmpty(thresholds)) {
        thresholds.forEach(threshold
            -> thresholdsDTOList.add(
                TimeSeriesMLTransactionThresholds.TimeSeriesMLTransactionThresholdsDTO.fromTransactionThresholdsEntity(
                    threshold)));
      }
      yaml.setMetricThresholds(thresholdsDTOList);
    }
  }

  public void toBean(ChangeContext<Y> changeContext, B bean, String appId, String yamlPath) {
    super.toBean(changeContext, bean, appId, yamlPath);
    List<TimeSeriesMLTransactionThresholdsDTO> thresholdsDTOList = changeContext.getYaml().getMetricThresholds();
    if (thresholdsDTOList != null) {
      String customThresholdRefId = isEmpty(changeContext.getYaml().getCustomThresholdRefId())
          ? generateUuid()
          : changeContext.getYaml().getCustomThresholdRefId();

      List<TimeSeriesMLTransactionThresholds> thresholdsInDB =
          metricDataAnalysisService.getCustomThreshold(customThresholdRefId);
      Set<String> thresholdIds =
          thresholdsInDB.stream().map(TimeSeriesMLTransactionThresholds::getUuid).collect(Collectors.toSet());

      List<TimeSeriesMLTransactionThresholds> thresholds = new ArrayList<>();
      thresholdsDTOList.forEach(dto -> thresholds.add(dto.toEntity(customThresholdRefId)));
      metricDataAnalysisService.saveCustomThreshold(null, bean.getUuid(), thresholds);
      if (isNotEmpty(thresholdIds)) {
        metricDataAnalysisService.deleteCustomThreshold(new ArrayList<>(thresholdIds));
      }
      bean.setCustomThresholdRefId(customThresholdRefId);
    }
  }
}
