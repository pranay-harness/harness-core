package software.wings.utils;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import software.wings.service.impl.elk.ElkLogFetchRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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

    ElkLogFetchRequest elkLogFetchRequest = new ElkLogFetchRequest(query, null, hosts, startTime, endTime);

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

    Assert.assertEquals(expectedJson, JsonUtils.asJson(elkLogFetchRequest.toElasticSearchJsonObject()));
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

    ElkLogFetchRequest elkLogFetchRequest = new ElkLogFetchRequest(query, indices, hosts, startTime, endTime);

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

    JSONObject indicesArray = new JSONObject();
    for (String index : indices.split(",")) {
      indicesArray.append("indices", index.trim());
    }

    indicesArray.put("query", new JSONObject().put("bool", mustArrayObjects));
    JSONObject indicesObject = new JSONObject().put("indices", indicesArray);

    String expectedJson = new JSONObject().put("query", indicesObject).toString();

    Assert.assertEquals(expectedJson, JsonUtils.asJson(elkLogFetchRequest.toElasticSearchJsonObject()));
  }
}
