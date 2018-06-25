package software.wings.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.Base;
import software.wings.beans.EmbeddedUser;
import software.wings.utils.JsonUtils;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;

/**
 * Created by rsingh on 1/8/18.
 */
@Entity(value = "thirdPartyApiCallLog")
@Data
@EqualsAndHashCode(callSuper = false, exclude = {"validUntil"})
@NoArgsConstructor
@ToString(exclude = {"validUntil"})
public class ThirdPartyApiCallLog extends Base implements Cloneable {
  private static final String NO_STATE_EXECUTION_ID = "NO_STATE_EXECUTION";
  private static final int MAX_JSON_RESPONSE_LENGTH = 512;
  private @NotEmpty @Indexed String stateExecutionId;
  private @NotEmpty @Indexed String accountId;
  private @NotEmpty String delegateId;
  private @NotEmpty String delegateTaskId;
  private @NotEmpty String request;
  private int statusCode;
  private String response;
  private long requestTimeStamp;
  private long responseTimeStamp;

  @SchemaIgnore
  @JsonIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(1).toInstant());

  @Builder
  public ThirdPartyApiCallLog(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, List<String> keywords, String entityYamlPath,
      String stateExecutionId, String accountId, String delegateId, String delegateTaskId, String request,
      int statusCode, String response, long requestTimeStamp, long responseTimeStamp) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, keywords, entityYamlPath);
    this.stateExecutionId = stateExecutionId;
    this.accountId = accountId;
    this.delegateId = delegateId;
    this.delegateTaskId = delegateTaskId;
    this.request = request;
    this.statusCode = statusCode;
    this.response = response;
    this.requestTimeStamp = requestTimeStamp;
    this.responseTimeStamp = responseTimeStamp;
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    return super.clone();
  }

  public void setJsonResponse(Object response) {
    String jsonResponse = JsonUtils.asJson(response);
    setResponse(jsonResponse.substring(
        0, jsonResponse.length() < MAX_JSON_RESPONSE_LENGTH ? jsonResponse.length() : MAX_JSON_RESPONSE_LENGTH));
  }

  public static ThirdPartyApiCallLog apiCallLogWithDummyStateExecution(String accountId) {
    return ThirdPartyApiCallLog.builder().accountId(accountId).stateExecutionId(NO_STATE_EXECUTION_ID).build();
  }
}
