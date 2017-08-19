package software.wings.service.impl.elk;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.json.JSONObject;
import software.wings.utils.JsonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by rsingh on 8/3/17.
 */

@Data
@AllArgsConstructor
public class ElkLogFetchRequest {
  private final String query;
  private final String indices;
  private final Set<String> hosts;
  private final long startTime;
  private final long endTime;

  public Object toElasticSearchJsonObject() {
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

    String jsonOut = new JSONObject().put("query", new JSONObject().put("bool", mustArrayObjects)).toString();
    return JsonUtils.asObject(jsonOut, Object.class);
  }

  public static void main(String[] args) {
    Set<String> hosts = new HashSet<>();
    hosts.add("cdcd");
    hosts.add("csdcd");

    ElkLogFetchRequest elkLogFetchRequest = new ElkLogFetchRequest("dssdcsd", "_all", hosts, 38465l, 98344l);
    Object jsonObject = elkLogFetchRequest.toElasticSearchJsonObject();
    System.out.println(jsonObject);
  }
}
