package software.wings.verification.apm;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.metrics.MetricType;
import software.wings.sm.states.APMVerificationState.MetricCollectionInfo;
import software.wings.verification.CVConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class APMCVServiceConfiguration extends CVConfiguration {
  private List<MetricCollectionInfo> metricCollectionInfos;

  @Override
  public CVConfiguration deepCopy() {
    APMCVServiceConfiguration clonedConfig = new APMCVServiceConfiguration();
    super.copy(clonedConfig);
    clonedConfig.setMetricCollectionInfos(this.getMetricCollectionInfos());
    return clonedConfig;
  }

  @Data
  @Builder
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  @JsonPropertyOrder({"type", "harnessApiVersion"})
  public static final class APMCVConfigurationYaml extends CVConfigurationYaml {
    private List<MetricCollectionInfo> metricCollectionInfos;
  }

  public boolean validate() {
    if (isEmpty(metricCollectionInfos) || isAllThroughput()) {
      return false;
    }
    return validateErrorOrResponseTime();
  }

  private boolean isAllThroughput() {
    if (isEmpty(metricCollectionInfos)) {
      return false;
    }
    AtomicBoolean isAllThroughput = new AtomicBoolean(true);
    metricCollectionInfos.forEach(metricCollectionInfo -> {
      if (!MetricType.THROUGHPUT.equals(metricCollectionInfo.getMetricType())) {
        isAllThroughput.set(false);
        return;
      }
    });
    return isAllThroughput.get();
  }

  public boolean validateUniqueMetricTxnCombination(List<MetricCollectionInfo> metricCollectionInfoList) {
    boolean isValidCombination = true;

    Map<String, String> metricNameTxnNameMap = new HashMap<>();

    if (isNotEmpty(metricCollectionInfoList)) {
      for (MetricCollectionInfo metricCollectionInfo : metricCollectionInfoList) {
        String txnName = metricCollectionInfo.getResponseMapping().getTxnNameFieldValue() == null
            ? "*"
            : metricCollectionInfo.getResponseMapping().getTxnNameFieldValue();

        if (metricNameTxnNameMap.containsKey(metricCollectionInfo.getMetricName())
            && metricNameTxnNameMap.get(metricCollectionInfo.getMetricName()).equals(txnName)) {
          isValidCombination = false;
          break;
        } else {
          metricNameTxnNameMap.put(metricCollectionInfo.getMetricName(), txnName);
        }
      }
      return isValidCombination;
    }
    return false;
  }

  private boolean validateErrorOrResponseTime() {
    AtomicBoolean isValidConfig = new AtomicBoolean(true);
    if (isEmpty(metricCollectionInfos)) {
      // not a valid configuration
      return false;
    }

    // First, basic validation. If there is atleast one error/Response time, there should be atleast one throughput.
    AtomicBoolean hasErrorOrTime = new AtomicBoolean(false), hasAtleastOneThroughput = new AtomicBoolean(false);
    metricCollectionInfos.forEach(metricCollectionInfo -> {
      if (MetricType.THROUGHPUT.equals(metricCollectionInfo.getMetricType())) {
        hasAtleastOneThroughput.set(true);
      }
      if (MetricType.ERROR.equals(metricCollectionInfo.getMetricType())
          || MetricType.RESP_TIME.equals(metricCollectionInfo.getMetricType())) {
        hasErrorOrTime.set(true);
      }
    });

    if (!hasErrorOrTime.get()) {
      return true;
    }
    if (!hasAtleastOneThroughput.get()) {
      return false;
    }

    // Now for some more stringent validation based on txnName if it's available.
    Map<String, List<MetricCollectionInfo>> txnNameInfoMap = new HashMap<>();
    metricCollectionInfos.forEach(metricCollectionInfo -> {
      final String txnName = metricCollectionInfo.getResponseMapping().getTxnNameFieldValue();
      if (isNotEmpty(txnName)) {
        if (!txnNameInfoMap.containsKey(txnName)) {
          txnNameInfoMap.put(txnName, new ArrayList<>());
        }
        txnNameInfoMap.get(txnName).add(metricCollectionInfo);
      }
    });

    if (isNotEmpty(txnNameInfoMap)) {
      txnNameInfoMap.forEach((txnName, metricInfoList) -> {
        // if this metricInfoList has an error/response time, it should have throughput also.
        hasErrorOrTime.set(false);
        hasAtleastOneThroughput.set(false);

        metricInfoList.forEach(metricInfo -> {
          if (MetricType.THROUGHPUT.equals(metricInfo.getMetricType())) {
            hasAtleastOneThroughput.set(true);
          }
          if (MetricType.ERROR.equals(metricInfo.getMetricType())
              || MetricType.RESP_TIME.equals(metricInfo.getMetricType())) {
            hasErrorOrTime.set(true);
          }
        });

        if (hasErrorOrTime.get() && !hasAtleastOneThroughput.get()) {
          // it has an error/time but no throughput
          isValidConfig.set(false);
          return;
        }
      });
    }
    return isValidConfig.get();
  }
}
