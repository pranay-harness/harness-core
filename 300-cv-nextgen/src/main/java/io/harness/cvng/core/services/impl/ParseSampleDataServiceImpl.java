package io.harness.cvng.core.services.impl;

import io.harness.cvng.core.beans.TimeSeriesSampleDTO;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.services.api.ParseSampleDataService;
import io.harness.datacollection.exception.DataCollectionDSLException;

import com.google.common.base.Preconditions;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JsonOrgJsonProvider;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;

@Slf4j
public class ParseSampleDataServiceImpl implements ParseSampleDataService {
  @Override
  public List<TimeSeriesSampleDTO> parseSampleData(ProjectParams projectParams, String jsonResponse, String groupName,
      String metricValueJsonPath, String timestampJsonPath, String timestampFormat) {
    try {
      List metricValueArr = compute(jsonResponse, metricValueJsonPath);
      List timestampArr = compute(jsonResponse, timestampJsonPath);

      Preconditions.checkState(metricValueArr.size() == timestampArr.size(),
          "List of metric values does not match the list of timestamps in the response.");
      List<TimeSeriesSampleDTO> parsedResponseList = new ArrayList<>();
      int lengthOfValues = metricValueArr.size();

      for (int i = 0; i < lengthOfValues; i++) {
        Long timestamp = parseTimestamp(timestampArr.get(i), timestampFormat);

        parsedResponseList.add(
            TimeSeriesSampleDTO.builder()
                .metricValue(metricValueArr.get(i) == null ? null : Double.valueOf(metricValueArr.get(i).toString()))
                .timestamp(timestamp)
                .txnName(groupName)
                .build());
      }

      return parsedResponseList;
    } catch (Exception ex) {
      log.error("Exception while parsing jsonObject {} and metricPath {} and timestampPath {}", jsonResponse,
          metricValueJsonPath, timestampJsonPath);
      throw new RuntimeException("Unable to parse the response object with the given json paths", ex);
    }
  }

  public List compute(String jsonValue, String jsonPath) {
    Object jsonObj;
    try {
      jsonObj = new JSONObject(jsonValue);
    } catch (Exception e) {
      jsonObj = new JSONArray(jsonValue);
    }

    Configuration conf = Configuration.defaultConfiguration()
                             .jsonProvider(new JsonOrgJsonProvider())
                             .addOptions(Option.SUPPRESS_EXCEPTIONS);
    JSONArray responseArray = JsonPath.using(conf).parse(jsonObj).read(jsonPath);
    return responseArray.toList();
  }

  private long getTimestampInMillis(long timestamp) {
    long now = Instant.now().toEpochMilli();
    if (timestamp != 0 && String.valueOf(timestamp).length() < String.valueOf(now).length()) {
      // Timestamp is in seconds. Convert to millis
      timestamp = timestamp * 1000;
    } else if (String.valueOf(timestamp).length() == String.valueOf(TimeUnit.MILLISECONDS.toNanos(now)).length()) {
      timestamp = TimeUnit.NANOSECONDS.toMillis(timestamp);
    }
    return timestamp;
  }

  private long parseTimestamp(Object timestampObj, String format) {
    long timestamp;
    if (timestampObj instanceof Long) {
      timestamp = (Long) timestampObj;
      timestamp = getTimestampInMillis(timestamp);
    } else if (timestampObj instanceof Double) {
      timestamp = ((Double) timestampObj).longValue();
      timestamp = getTimestampInMillis(timestamp);
    } else if (timestampObj instanceof Integer) {
      timestamp = ((Integer) timestampObj).longValue();
      timestamp = getTimestampInMillis(timestamp);
    } else {
      try {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        Date date = simpleDateFormat.parse((String) timestampObj);
        timestamp = date.toInstant().toEpochMilli();
      } catch (ParseException e) {
        throw new DataCollectionDSLException("Unable to parse timestamp", e);
      }
    }
    return timestamp;
  }
}
