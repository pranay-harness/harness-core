package software.wings.service.impl.elk;

import static java.util.Arrays.asList;

import io.harness.exception.WingsException;
import lombok.Builder;
import lombok.Data;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.utils.JsonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * Created by rsingh on 8/3/17.
 */

@Data
@Builder
public class ElkLogFetchRequest {
  private final String query;
  private final String indices;
  private final String hostnameField;
  private final String messageField;
  private final String timestampField;
  private final Set<String> hosts;
  private final long startTime;
  private final long endTime;
  @Builder.Default private final boolean formattedQuery = false;
  @Builder.Default private ElkQueryType queryType = ElkQueryType.TERM;
  private static final Logger logger = LoggerFactory.getLogger(ElkLogFetchRequest.class);

  public Object toElasticSearchJsonObject() {
    List<JSONObject> hostJsonObjects = new ArrayList<>();
    for (String host : hosts) {
      hostJsonObjects.add(
          new JSONObject().put(queryType.name().toLowerCase(), new JSONObject().put(hostnameField, host)));
    }

    JSONObject regexObject = new JSONObject();
    regexObject.put("regexp", new JSONObject().put(messageField, new JSONObject().put("value", query.toLowerCase())));

    JSONObject rangeObject = new JSONObject();
    rangeObject.put("range",
        new JSONObject().put(
            timestampField, new JSONObject().put("gte", startTime).put("lt", endTime).put("format", "epoch_millis")));

    Map<String, List<JSONObject>> mustArrayObjects = new HashMap<>();
    mustArrayObjects.put("filter",
        asList(new JSONObject().put("bool", new JSONObject().put("should", hostJsonObjects)), rangeObject,
            formattedQuery ? getJSONQuery() : eval()));

    JSONObject queryObject =
        new JSONObject().put("query", new JSONObject().put("bool", mustArrayObjects)).put("size", 10000);
    String jsonOut = queryObject.toString();

    return JsonUtils.asObject(jsonOut, Object.class);
  }

  public static Object lastInsertedRecordObject(boolean shouldSort) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("size", 1);

    jsonObject.put("query", new JSONObject().put("match_all", new HashMap<>()));
    if (shouldSort) {
      jsonObject.put("sort", new JSONObject().put("@timestamp", "desc"));
    }

    return JsonUtils.asObject(jsonObject.toString(), Object.class);
  }

  private String insertSpaces(String expr) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < expr.length(); i++) {
      if (expr.charAt(i) == '(') {
        result.append(expr.charAt(i));
        result.append(' ');
      } else if (expr.charAt(i) == ')') {
        result.append(' ');
        result.append(expr.charAt(i));
      } else {
        result.append(expr.charAt(i));
      }
    }
    return result.toString();
  }

  protected JSONObject eval() {
    try {
      String[] tokens = insertSpaces(query.toLowerCase()).split(" ");
      Stack<JSONObject> operandStack = new Stack<>();
      Stack<String> operatorStack = new Stack<>();
      JSONObject rval, lval;
      String operator;
      for (String token : tokens) {
        if (")".equals(token)) {
          Stack<JSONObject> result = new Stack<>();
          while (!(rval = operandStack.pop())
                      .toString()
                      .equals("{\"" + queryType.name().toLowerCase() + "\":{\"(\":\"(\"}}")) {
            if (operatorStack.isEmpty()) {
              result.push(rval);
            } else {
              lval = operandStack.pop();
              operator = operatorStack.pop();
              result.push(eval(lval, rval, operator));
            }
          }
          result.forEach(op -> operandStack.push(op));
        } else {
          if ("or".equals(token.toLowerCase()) || "and".equals(token.toLowerCase())) {
            operatorStack.push(token);
          } else {
            operandStack.push(stringToJson(token));
          }
        }
      }

      while (!operatorStack.empty()) {
        rval = operandStack.pop();
        lval = operandStack.pop();
        operator = operatorStack.pop();
        operandStack.push(eval(lval, rval, operator));
      }

      if (operandStack.size() > 1) {
        List<JSONObject> mustObjectList = new ArrayList<>();
        while (!operandStack.isEmpty()) {
          if (operandStack.peek().toString().equals("{\"" + queryType.name().toLowerCase() + "\":{\"(\":\"(\"}}")) {
            throw new WingsException("Unmatched open braces `(`");
          }
          mustObjectList.add(operandStack.pop());
        }
        return new JSONObject().put("bool", new JSONObject().put("must", mustObjectList));
      }
      return operandStack.pop();

    } catch (RuntimeException ex) {
      throw new WingsException(
          "Malformed Query. Braces should be matching. Only supported operators are 'or' and 'and' ");
    }
  }

  private JSONObject getJSONQuery() {
    try {
      return new JSONObject(query);
    } catch (JSONException ex) {
      logger.error("Given query is not json formatted");
      throw new WingsException("Invalid JSON Query Passed : " + query);
    }
  }

  private JSONObject eval(JSONObject lval, JSONObject rval, String operator) {
    if ("or".equals(operator.toLowerCase())) {
      return evalOrExpression(lval, rval);
    } else if ("and".equals(operator.toLowerCase())) {
      return evalAndExpression(lval, rval);
    } else {
      throw new WingsException("Unknown operator in expression " + operator);
    }
  }

  private JSONObject evalOrExpression(JSONObject left, JSONObject right) {
    List<JSONObject> jsonObjects = asList(left, right);
    return new JSONObject().put("bool", new JSONObject().put("should", jsonObjects));
  }

  private JSONObject evalAndExpression(JSONObject left, JSONObject right) {
    List<JSONObject> jsonObjects = asList(left, right);
    return new JSONObject().put("bool", new JSONObject().put("must", jsonObjects));
  }

  private JSONObject stringToJson(String str) {
    if (str.equals("(") || str.equals(")")) {
      return termToJson(str, str);
    } else if (str.contains(":")) {
      String[] terms = str.split(":");
      if (terms.length > 2) {
        throw new WingsException("Cannot parse " + str + " . Should be value or key:value");
      }
      return termToJson(terms[0], terms[1]);
    } else {
      return messageToJson(messageField, str);
    }
  }

  private JSONObject messageToJson(String term, String val) {
    JSONObject regexObject = new JSONObject();
    regexObject.put("regexp", new JSONObject().put(term, new JSONObject().put("value", val)));
    return regexObject;
  }

  private JSONObject termToJson(String term, String val) {
    return new JSONObject().put(queryType.name().toLowerCase(), new JSONObject().put(term, val));
  }
}
