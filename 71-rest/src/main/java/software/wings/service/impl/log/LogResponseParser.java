package software.wings.service.impl.log;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.common.collect.Multimap;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.apm.VerificationResponseParser;
import software.wings.sm.states.CustomLogVerificationState.ResponseMapper;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LogResponseParser {
  private static final Logger logger = LoggerFactory.getLogger(LogResponseParser.class);
  private static final String TIMESTAMP_FIELD = "timestamp";
  private static final String DEFAULT_TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
  private static final int MAX_CONCAT_JSON_FIELDS = 100;
  @Data
  @AllArgsConstructor
  @Builder
  public static class LogResponseData {
    String responseText;
    Set<String> hostList;
    boolean shouldInspectHosts;
    private Map<String, ResponseMapper> responseMappers;
  }

  public List<LogElement> extractLogs(LogResponseData data) {
    Map<String, LogElement> resultMap = new HashMap<>();
    VerificationResponseParser logsResponseParser = new VerificationResponseParser();
    for (ResponseMapper responseMapper : data.getResponseMappers().values()) {
      if (!isEmpty(responseMapper.getJsonPath()) && !isEmpty(responseMapper.getJsonPath().get(0))) {
        logsResponseParser.put(responseMapper.getJsonPath().get(0).split("\\."), responseMapper.getFieldName(),
            responseMapper.getRegexs());
      }
      if (responseMapper.getJsonPath().size() > 1) {
        for (int index = 1; index < responseMapper.getJsonPath().size(); index++) {
          String path = responseMapper.getJsonPath().get(index);
          if (!isEmpty(path)) {
            logsResponseParser.put(
                path.split("\\."), responseMapper.getFieldName() + index, responseMapper.getRegexs());
          }
        }
      }
    }
    List<Multimap<String, Object>> output = null;
    try {
      logger.info("Response was {}", data.responseText);
      output = logsResponseParser.extract(data.responseText);
    } catch (Exception ex) {
      logger.error("Unable to extract data in LogResponseParser {}", data.responseText);
    }
    createRecords(output, resultMap);
    // filter only the hosts we care about
    List<LogElement> logs = new ArrayList<>();
    if (resultMap.size() > 0 && data.shouldInspectHosts) {
      for (LogElement logElement : resultMap.values()) {
        if (data.hostList.contains(logElement.getHost())) {
          logs.add(logElement);
        }
      }
    } else if (!data.shouldInspectHosts) {
      logs.addAll(resultMap.values());
    }
    return logs;
  }

  private static void createRecords(List<Multimap<String, Object>> response, Map<String, LogElement> resultMap) {
    if (response == null) {
      logger.error("Something went wrong during parsing. Response is null.");
      return;
    }
    for (Multimap<String, Object> record : response) {
      Iterator<Object> timestamps = record.get(TIMESTAMP_FIELD).iterator();
      List<Iterator<Object>> logMessages = new ArrayList<>();
      logMessages.add(record.get("logMessage").iterator());
      for (int index = 1; index < MAX_CONCAT_JSON_FIELDS; index++) {
        if (!record.containsKey("logMessage" + index)) {
          break;
        }
        logMessages.add(record.get("logMessage" + index).iterator());
      }
      Iterator<Object> hostnames = record.get("host").iterator();
      while (timestamps.hasNext()) {
        String timestampStr = (String) timestamps.next();
        DateTimeFormatter df = DateTimeFormatter.ofPattern(DEFAULT_TIMESTAMP_FORMAT);
        long timestamp = Instant.from(df.parse(timestampStr)).toEpochMilli();
        String hostName = record.containsKey("host") ? (String) hostnames.next() : null;
        final String key = timestamp + ":" + hostName;
        if (!resultMap.containsKey(key)) {
          resultMap.put(key, new LogElement());
          resultMap.get(key).setTimeStamp(timestamp);
          resultMap.get(key).setCount(1);

          // We do this if we need to concatenate multiple json paths to the log message
          StringBuffer sBuf = new StringBuffer();
          for (Iterator<Object> logIter : logMessages) {
            Object msg = logIter.next();
            String msgStr = "";
            if (!(msg instanceof String)) {
              msgStr = msg.toString();
            } else {
              msgStr = (String) msg;
            }
            sBuf.append(msgStr);
            sBuf.append(", ");
          }
          resultMap.get(key).setLogMessage(sBuf.toString());

          resultMap.get(key).setHost(hostName);
        }
      }
    }
  }
}
