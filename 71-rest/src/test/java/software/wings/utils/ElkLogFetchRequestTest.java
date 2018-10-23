package software.wings.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static software.wings.delegatetasks.ElkLogzDataCollectionTask.parseElkResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.junit.Test;
import software.wings.delegatetasks.ElkLogzDataCollectionTask;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.elk.ElkLogFetchRequest;
import software.wings.service.impl.elk.ElkQueryType;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 8/3/17.
 */
public class ElkLogFetchRequestTest {
  @Test
  public void testJsonFormat() {
    Set<String> hosts = new HashSet<>();
    hosts.add("cdcd");
    hosts.add("csdcd");
    String query = "dskcdsds";
    String indices = "_all";
    long startTime = System.currentTimeMillis();
    long endTime = startTime + TimeUnit.HOURS.toMillis(34);

    ElkLogFetchRequest elkLogFetchRequest = ElkLogFetchRequest.builder()
                                                .query(query)
                                                .indices(null)
                                                .hostnameField("beat.hostname")
                                                .messageField("message")
                                                .timestampField("")
                                                .hosts(hosts)
                                                .startTime(startTime)
                                                .endTime(endTime)
                                                .queryType(ElkQueryType.TERM)
                                                .build();

    List<JSONObject> hostJsonObjects = new ArrayList<>();
    for (String host : hosts) {
      hostJsonObjects.add(new JSONObject().put("term", new JSONObject().put("beat.hostname", host)));
    }

    JSONObject boolObject = new JSONObject().put("bool", new JSONObject().put("should", hostJsonObjects));

    JSONObject regexObject = new JSONObject();
    regexObject.put("regexp", new JSONObject().put("message", new JSONObject().put("value", query)));

    JSONObject rangeObject = new JSONObject();
    rangeObject.put("range",
        new JSONObject().put(
            "@timestamp", new JSONObject().put("gte", startTime).put("lt", endTime).put("format", "epoch_millis")));

    Map<String, List<JSONObject>> mustArrayObjects = new HashMap<>();
    mustArrayObjects.put("must", new ArrayList<>());
    mustArrayObjects.get("must").add(regexObject);
    mustArrayObjects.get("must").add(boolObject);
    mustArrayObjects.get("must").add(rangeObject);

    String expectedJson = new JSONObject().put("query", new JSONObject().put("bool", mustArrayObjects)).toString();

    //    assertEquals(expectedJson, JsonUtils.asJson(elkLogFetchRequest.toElasticSearchJsonObject()));
  }

  @Test
  public void testJsonFormatWithIndices() {
    Set<String> hosts = new HashSet<>();
    hosts.add("cdcd");
    hosts.add("csdcd");
    String query = "dskcdsds";
    String indices = " _all, _none ";
    long startTime = System.currentTimeMillis();
    long endTime = startTime + TimeUnit.HOURS.toMillis(34);

    ElkLogFetchRequest elkLogFetchRequest = ElkLogFetchRequest.builder()
                                                .query(query)
                                                .indices(indices)
                                                .hostnameField("beat.hostname")
                                                .messageField("message")
                                                .timestampField("")
                                                .hosts(hosts)
                                                .startTime(startTime)
                                                .endTime(endTime)
                                                .queryType(ElkQueryType.MATCH)
                                                .build();

    List<JSONObject> hostJsonObjects = new ArrayList<>();
    for (String host : hosts) {
      hostJsonObjects.add(new JSONObject().put("match", new JSONObject().put("beat.hostname", host)));
    }

    JSONObject boolObject = new JSONObject().put("bool", new JSONObject().put("should", hostJsonObjects));

    JSONObject regexObject = new JSONObject();
    regexObject.put("regexp", new JSONObject().put("message", new JSONObject().put("value", query)));

    JSONObject rangeObject = new JSONObject();
    rangeObject.put("range",
        new JSONObject().put(
            "@timestamp", new JSONObject().put("gte", startTime).put("lt", endTime).put("format", "epoch_millis")));

    Map<String, List<JSONObject>> mustArrayObjects = new HashMap<>();
    mustArrayObjects.put("must", new ArrayList<>());
    mustArrayObjects.get("must").add(regexObject);
    mustArrayObjects.get("must").add(boolObject);
    mustArrayObjects.get("must").add(rangeObject);

    JSONObject indicesArray = new JSONObject();
    for (String index : indices.split(",")) {
      indicesArray.append("indices", index.trim());
    }

    indicesArray.put("query", new JSONObject().put("bool", mustArrayObjects));
    JSONObject indicesObject = new JSONObject().put("indices", indicesArray);

    String expectedJson = new JSONObject().put("query", indicesObject).toString();

    //    assertEquals(expectedJson, JsonUtils.asJson(elkLogFetchRequest.toElasticSearchJsonObject()));
  }

  @Test
  public void testParse() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    LinkedHashMap map = mapper.readValue(
        new File(getClass().getClassLoader().getResource("./elk/elk.txt").getFile()), LinkedHashMap.class);
    List<LogElement> logElements = parseElkResponse(map, "info", "@timestamp", "yyyy-MM-dd'T'HH:mm:ss.SSSX",
        "kubernetes.pod.name", "harness-learning-engine", "log", 0, false);
    assertFalse(logElements.isEmpty());
    assertEquals("Hostname should be correct", "rddashboard-prod-5-67d88f4657-ff7k9", logElements.get(0).getHost());
  }

  @Test
  public void testJsonParsing() {
    String jsonString =
        "{\"a\" : \"this is a test\", \"b\":[\"test1\", \"test2\", \"test3\"], \"c\":{ \"d\": \"another test\"}}";
    JSONObject jsonObject = new JSONObject(jsonString);
    String aValue = ElkLogzDataCollectionTask.parseAndGetValue(jsonObject, "a");
    String bValue = ElkLogzDataCollectionTask.parseAndGetValue(jsonObject, "b.0");
    String cValue = ElkLogzDataCollectionTask.parseAndGetValue(jsonObject, "c.d");

    assertEquals("aValue should be same", "this is a test", aValue);
    assertEquals("bValue should be same", "test1", bValue);
    assertEquals("cValue should be same", "another test", cValue);
  }
}
