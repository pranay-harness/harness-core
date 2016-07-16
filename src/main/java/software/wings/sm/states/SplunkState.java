package software.wings.sm.states;

import static com.google.common.collect.ImmutableSortedMap.of;
import static software.wings.api.SplunkStateExecutionData.Builder.aSplunkStateExecutionData;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.apache.commons.codec.binary.Base64;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.HttpStateExecutionData;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingValue.SettingVariableTypes;
import software.wings.beans.SplunkConfig;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Created by peeyushaggarwal on 7/15/16.
 */
public class SplunkState extends HttpState {
  private static final Logger logger = LoggerFactory.getLogger(HttpState.class);

  @Transient @Inject private SettingsService settingsService;

  @Attributes(required = true, title = "Query") private String query;

  /**
   * Create a new Http State with given name.
   *
   * @param name name of the state.
   */
  public SplunkState(String name) {
    super(name);
    setStateType(StateType.SPLUNK.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    String evaluatedQuery = context.renderExpression(query);
    logger.info("evaluatedQuery: {}", evaluatedQuery);

    SettingAttribute splunkSettingAttribute =
        settingsService.getGlobalSettingAttributesByType(SettingVariableTypes.SPLUNK).get(0);
    SplunkConfig splunkConfig = (SplunkConfig) splunkSettingAttribute.getValue();

    setUrl("https://" + splunkConfig.getHost() + ":" + splunkConfig.getPort() + "/services/search/jobs");
    setMethod("POST");
    try {
      setBody(toPostBody(of("search", "search " + evaluatedQuery, "exec_mode", "oneshot")));
    } catch (UnsupportedEncodingException e) {
      logger.error("Exception: ", e);
      return anExecutionResponse()
          .withErrorMessage(e.getMessage())
          .withExecutionStatus(ExecutionStatus.ERROR)
          .withStateExecutionData(
              aSplunkStateExecutionData().withQuery(evaluatedQuery).withAssertionStatement(getAssertion()).build())
          .build();
    }

    setHeader("Authorization: Basic "
        + Base64.encodeBase64URLSafeString(
              (splunkConfig.getUsername() + ":" + splunkConfig.getPassword()).getBytes(StandardCharsets.UTF_8)));

    ExecutionResponse executionResponse = super.execute(context);

    HttpStateExecutionData httpStateExecutionData = (HttpStateExecutionData) executionResponse.getStateExecutionData();
    executionResponse.setStateExecutionData(aSplunkStateExecutionData()
                                                .withQuery(evaluatedQuery)
                                                .withAssertionStatement(getAssertion())
                                                .withAssertionStatus(httpStateExecutionData.getAssertionStatus())
                                                .withResponse(httpStateExecutionData.getHttpResponseBody())
                                                .build());

    return executionResponse;
  }

  /**
   * Getter for property 'query'.
   *
   * @return Value for property 'query'.
   */
  public String getQuery() {
    return query;
  }

  /**
   * Setter for property 'query'.
   *
   * @param query Value to set for property 'query'.
   */
  public void setQuery(String query) {
    this.query = query;
  }

  @SchemaIgnore
  @Override
  public String getBody() {
    return super.getBody();
  }

  @SchemaIgnore
  @Override
  public String getHeader() {
    return super.getHeader();
  }

  @SchemaIgnore
  @Override
  public String getMethod() {
    return super.getMethod();
  }

  @SchemaIgnore
  @Override
  public String getUrl() {
    return super.getUrl();
  }

  @Attributes(required = true, title = "Assertion")
  @Override
  public String getAssertion() {
    return super.getAssertion();
  }

  private String toPostBody(Map<String, String> params) throws UnsupportedEncodingException {
    StringBuilder postData = new StringBuilder();
    for (Entry<String, String> param : params.entrySet()) {
      if (postData.length() != 0)
        postData.append('&');
      postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
      postData.append('=');
      postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
    }
    return postData.toString();
  }
}
