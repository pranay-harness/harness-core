package software.wings.service.impl.apm;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.expression.RegexFunctor;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class APMResponseParser {
  @Data
  @AllArgsConstructor
  @Builder
  public static class APMResponseData {
    private String hostName;
    private String text;
    private List<APMMetricInfo> metricInfos;
  }

  private static Pattern p = Pattern.compile("\\[(.*?)\\]");
  private static RegexFunctor regexFunctor = new RegexFunctor();
  private static final Logger logger = LoggerFactory.getLogger(APMResponseData.class);
  private List<String> value;
  private Map<String, APMResponseParser> children;
  private List<List<String>> regex;

  public APMResponseParser() {
    children = new HashMap<>();
  }

  public void put(String[] keys, String value) {
    putHelper(this, keys, value, 0, Collections.EMPTY_LIST);
  }

  public void put(String[] keys, String value, List<String> regex) {
    putHelper(this, keys, value, 0, regex);
  }

  private void putHelper(APMResponseParser node, String[] keys, String value, int index, List<String> regex) {
    if (index == keys.length) {
      if (node.value == null) {
        node.value = new ArrayList<>();
        node.regex = new ArrayList<>();
      }
      node.value.add(value);
      if (regex != null) {
        node.regex.add(regex);
      } else {
        node.regex.add(Collections.EMPTY_LIST);
      }
      return;
    }
    if (!node.children.containsKey(keys[index])) {
      node.children.put(keys[index], new APMResponseParser());
    }
    putHelper(node.children.get(keys[index]), keys, value, index + 1, regex);
  }

  public static Collection<NewRelicMetricDataRecord> extract(List<APMResponseData> apmResponseData) {
    Map<String, NewRelicMetricDataRecord> resultMap = new HashMap<>();
    for (APMResponseData data : apmResponseData) {
      for (APMMetricInfo metricInfo : data.getMetricInfos()) {
        APMResponseParser apmResponseParser = new APMResponseParser();
        for (APMMetricInfo.ResponseMapper responseMapper : metricInfo.getResponseMappers().values()) {
          if (!isEmpty(responseMapper.getJsonPath())) {
            apmResponseParser.put(
                responseMapper.getJsonPath().split("\\."), responseMapper.getFieldName(), responseMapper.getRegexs());
          }
        }
        List<Multimap<String, Object>> output = apmResponseParser.extract(data.text);
        createRecords(metricInfo.getResponseMappers().get("txnName").getFieldValue(), metricInfo.getMetricName(),
            data.hostName, metricInfo.getTag(), output, resultMap);
      }
    }
    return resultMap.values();
  }

  private List<Multimap<String, Object>> extract(String text) {
    Object jsonObject =
        text.charAt(0) == '[' && text.charAt(text.length() - 1) == ']' ? new JSONArray(text) : new JSONObject(text);
    List<Multimap<String, Object>> output = new ArrayList<>();
    /*
     * Pass in the root as level 1. There are 2 secnarios.
     *
     * scenario a) no common ancestor
     *
     *                  .  ====> make level 1
     *                / | \
     *           scope ts value
     *
     *
     * scenario b) single common ancestor. make this node level 1
     *
     *                   .  ====> make level 0
     *                   |
     *                series[*] ===> make level 1
     *				/		\
     *			scope		pointlist[*]
     *						/			\
     *					   0			1
     */
    // scenario a
    if (children.size() > 1) {
      process(this, jsonObject, 1, output);
    } else { // scenario b
      process(this, jsonObject, 0, output);
    }

    return output;
  }

  private static Object cast(Object val, String field) {
    if (field.equals("timestamp")) {
      if (val instanceof Double) {
        return ((Double) val).longValue();
      } else if (val instanceof Integer) {
        return ((Integer) val).longValue();
      } else {
        return val;
      }
    } else if (field.equals("value")) {
      if (val.toString().equals("null")) {
        return -1.0;
      }
      if (val instanceof Integer) {
        return ((Integer) val).doubleValue();
      } else if (val instanceof Long) {
        return ((Long) val).doubleValue();
      } else {
        return val;
      }
    }

    return val;
  }

  @SuppressFBWarnings("BX_UNBOXING_IMMEDIATELY_REBOXED")
  private static void createRecords(String txnName, String metricName, String hostName, String tag,
      List<Multimap<String, Object>> response, Map<String, NewRelicMetricDataRecord> resultMap) {
    for (Multimap<String, Object> record : response) {
      Iterator<Object> timestamps = record.get("timestamp").iterator();
      Iterator<Object> values = record.get("value").iterator();
      while (timestamps.hasNext()) {
        long timestamp = (long) cast(timestamps.next(), "timestamp");
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
          resultMap.get(key).setGroupName(NewRelicMetricDataRecord.DEFAULT_GROUP_NAME);
        }

        Object val = values.next();
        metricName =
            record.containsKey("metricName") ? (String) record.get("metricName").iterator().next() : metricName;

        resultMap.get(key).getValues().put(metricName, (double) cast(val, "value"));
      }
    }
  }

  private static Multimap<String, Object> process(
      APMResponseParser node, Object body, int level, List<Multimap<String, Object>> output) {
    Multimap<String, Object> result = ArrayListMultimap.create();
    Iterator<Map.Entry<String, APMResponseParser>> iterator = node.children.entrySet().iterator();
    //  no children leaf node
    if (!iterator.hasNext()) {
      for (int i = 0; i < node.value.size(); ++i) {
        Object evaluatedBody = body;
        if (!isEmpty(node.regex)) {
          for (String expr : node.regex.get(i)) {
            evaluatedBody = regexFunctor.extract(expr, (String) evaluatedBody);
          }
        }
        result.put(node.value.get(i), evaluatedBody);
      }
    } else {
      while (iterator.hasNext()) {
        Map.Entry<String, APMResponseParser> trieEntry = iterator.next();
        APMResponseParser childNode = trieEntry.getValue();
        Object childBody = getValue(body, trieEntry.getKey());
        if (childBody instanceof JSONArray) {
          for (Object val : (JSONArray) childBody) {
            result.putAll(process(childNode, val, level + 1, output));
          }
        } else {
          result.putAll(process(childNode, childBody, level + 1, output));
        }
      }
    }
    if (level == 1) {
      output.add(result);
    }
    return result;
  }

  @SuppressFBWarnings("DM_BOXED_PRIMITIVE_FOR_PARSING")
  private static Object getValue(Object jsonObject, String field) {
    if (field.contains("[")) {
      if (field.charAt(0) == '[') {
        Matcher matcher = p.matcher(field);
        matcher.find();
        String group = matcher.group(1);
        if (group.equals("*")) {
          return jsonObject;
        }
        Object val = ((JSONArray) jsonObject).get(Integer.valueOf(group));
        if (val instanceof JSONArray) {
          return ((JSONArray) jsonObject).getJSONArray(Integer.valueOf(group));
        }
        return val;

      } else {
        return ((JSONObject) jsonObject).getJSONArray(field.substring(0, field.length() - 3));
      }
    } else {
      return ((JSONObject) jsonObject).get(field);
    }
  }
}
