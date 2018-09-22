package software.wings.service.impl.logs;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import org.junit.Test;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.log.LogResponseParser;
import software.wings.service.impl.log.LogResponseParser.LogResponseData;
import software.wings.sm.states.CustomLogVerificationState;
import software.wings.sm.states.CustomLogVerificationState.ResponseMapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class LogResponseParserTest {
  @Test
  public void testParseValidResponse() throws IOException {
    String textLoad =
        Resources.toString(LogResponseParserTest.class.getResource("/apm/sampleElkResponse.json"), Charsets.UTF_8);

    List<String> hostList = Arrays.asList("harness-manager-1.0.5909-192-7bd4987549-pfwgn");
    Map<String, ResponseMapper> responseMappers = new HashMap<>();
    responseMappers.put("timestamp",
        CustomLogVerificationState.ResponseMapper.builder()
            .fieldName("timestamp")
            .jsonPath(Arrays.asList("@timestamp"))
            .timestampFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
            .build());
    responseMappers.put("host",
        CustomLogVerificationState.ResponseMapper.builder()
            .fieldName("host")
            .jsonPath(Arrays.asList("kubernetes.pod.name"))
            .build());
    responseMappers.put("logMessage",
        CustomLogVerificationState.ResponseMapper.builder()
            .fieldName("logMessage")
            .jsonPath(Arrays.asList("log"))
            .build());

    LogResponseParser parser = new LogResponseParser();
    LogResponseParser.LogResponseData data =
        new LogResponseData(textLoad, new HashSet<>(hostList), false, responseMappers);

    List<LogElement> logs = parser.extractLogs(data);
    assertNotNull(logs);
  }

  @Test
  public void testParseValidResponseMultiple() throws IOException {
    String textLoad = Resources.toString(
        LogResponseParserTest.class.getResource("/apm/elkMultipleHitsResponse.json"), Charsets.UTF_8);

    List<String> hostList = Arrays.asList("harness-manager-1.0.5922-198-b5bbc487d-bcqx6",
        "harness-manager-1.0.5919-197-bbd6df89f-mzrw2", "harness-manager-1.0.5922-198-b5bbc487d-rqx6p",
        "harness-manager-1.0.5922-199-656965f8ff-pcq4j", "harness-manager-1.0.5919-197-bbd6df89f-bg57m");
    Map<String, ResponseMapper> responseMappers = new HashMap<>();
    responseMappers.put("timestamp",
        CustomLogVerificationState.ResponseMapper.builder()
            .fieldName("timestamp")
            .jsonPath(Arrays.asList("hits.hits[*]._source.@timestamp"))
            .timestampFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
            .build());
    responseMappers.put("host",
        CustomLogVerificationState.ResponseMapper.builder()
            .fieldName("host")
            .jsonPath(Arrays.asList("hits.hits[*]._source.kubernetes.pod.name"))
            .build());
    responseMappers.put("logMessage",
        CustomLogVerificationState.ResponseMapper.builder()
            .fieldName("logMessage")
            .jsonPath(Arrays.asList("hits.hits[*]._source.log"))
            .build());

    LogResponseParser parser = new LogResponseParser();
    LogResponseParser.LogResponseData data =
        new LogResponseData(textLoad, new HashSet<>(hostList), true, responseMappers);

    List<LogElement> logs = parser.extractLogs(data);
    assertNotNull(logs);
    assertTrue("Log list should be 5 elements long", 5 == logs.size());
  }
}
