package software.wings.service.impl.apm;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.common.collect.Multimap;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.exception.WingsException;
import io.harness.time.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class APMResponseParser {
  @Data
  @AllArgsConstructor
  @Builder
  public static class APMResponseData {
    private String hostName;
    private String groupName;
    private String text;
    private List<APMMetricInfo> metricInfos;
  }

  private static final Logger logger = LoggerFactory.getLogger(APMResponseData.class);

  public static Collection<NewRelicMetricDataRecord> extract(List<APMResponseData> apmResponseData) {
    Map<String, NewRelicMetricDataRecord> resultMap = new HashMap<>();
    for (APMResponseData data : apmResponseData) {
      logger.info("Response Data is :  {}", data);
      for (APMMetricInfo metricInfo : data.getMetricInfos()) {
        VerificationResponseParser apmResponseParser = new VerificationResponseParser();
        for (APMMetricInfo.ResponseMapper responseMapper : metricInfo.getResponseMappers().values()) {
          if (!isEmpty(responseMapper.getJsonPath())) {
            apmResponseParser.put(
                responseMapper.getJsonPath().split("\\."), responseMapper.getFieldName(), responseMapper.getRegexs());
          }
        }
        List<Multimap<String, Object>> output = null;
        try {
          output = apmResponseParser.extract(data.text);
        } catch (Exception ex) {
          logger.warn("Unable to extract data in APM ResponseParser {}", data.text);
          continue;
        }
        createRecords(metricInfo.getResponseMappers().get("txnName").getFieldValue(), metricInfo.getMetricName(),
            data.hostName, metricInfo.getTag(), data.groupName, output, resultMap);
      }
    }
    return resultMap.values();
  }

  @SuppressFBWarnings("BX_UNBOXING_IMMEDIATELY_REBOXED")
  private static void createRecords(String txnName, String metricName, String hostName, String tag, String groupName,
      List<Multimap<String, Object>> response, Map<String, NewRelicMetricDataRecord> resultMap) {
    if (groupName == null) {
      final String errorMsg =
          "Unexpected null groupName received while parsing APMResponse. Please contact Harness Support.";
      logger.error(errorMsg);
      throw new WingsException(errorMsg);
    }
    for (Multimap<String, Object> record : response) {
      Iterator<Object> timestamps = record.get("timestamp").iterator();
      Iterator<Object> values = record.get("value").iterator();
      while (timestamps.hasNext()) {
        long timestamp = (long) VerificationResponseParser.cast(timestamps.next(), "timestamp");
        long now = Timestamp.currentMinuteBoundary();
        if (String.valueOf(timestamp).length() < String.valueOf(now).length()) {
          // Timestamp is in seconds. Convert to millis
          timestamp = timestamp * 1000;
        }
        txnName = record.containsKey("txnName") ? (String) record.get("txnName").iterator().next() : txnName;
        hostName = record.containsKey("host") ? (String) record.get("host").iterator().next() : hostName;
        String key = timestamp + ":" + txnName + ":" + hostName;
        if (!resultMap.containsKey(key)) {
          resultMap.put(key, new NewRelicMetricDataRecord());
          resultMap.get(key).setTimeStamp(timestamp);
          resultMap.get(key).setValues(new HashMap());
          resultMap.get(key).setName(txnName);
          resultMap.get(key).setHost(hostName);
          resultMap.get(key).setTag(tag);
          resultMap.get(key).setGroupName(groupName);
        }

        Object val = values.next();
        metricName =
            record.containsKey("metricName") ? (String) record.get("metricName").iterator().next() : metricName;

        resultMap.get(key).getValues().put(metricName, (double) VerificationResponseParser.cast(val, "value"));
      }
    }
  }
}
