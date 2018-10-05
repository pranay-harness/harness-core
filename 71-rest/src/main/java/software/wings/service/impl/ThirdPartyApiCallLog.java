package software.wings.service.impl;

import static software.wings.common.Constants.RESPONSE_BODY;
import static software.wings.common.Constants.STATUS_CODE;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.http.HttpStatus;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.Base;
import software.wings.beans.EmbeddedUser;
import software.wings.sm.ExecutionStatus;
import software.wings.utils.JsonUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class ThirdPartyApiCallLog extends Base {
  private static final String NO_STATE_EXECUTION_ID = "NO_STATE_EXECUTION";
  private static final int MAX_JSON_RESPONSE_LENGTH = 16384;
  private @NotEmpty @Indexed String stateExecutionId;
  private @NotEmpty @Indexed String accountId;
  private @NotEmpty String delegateId;
  private @NotEmpty String delegateTaskId;
  private String title;
  @Default private List<ThirdPartyApiCallField> request = new ArrayList<>();
  @Default private List<ThirdPartyApiCallField> response = new ArrayList<>();
  private long requestTimeStamp;
  private long responseTimeStamp;

  @SchemaIgnore
  @JsonIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(1).toInstant());

  @Builder
  public ThirdPartyApiCallLog(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, List<String> keywords, String entityYamlPath,
      String stateExecutionId, String accountId, String delegateId, String delegateTaskId, String title,
      List<ThirdPartyApiCallField> request, List<ThirdPartyApiCallField> response, long requestTimeStamp,
      long responseTimeStamp) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, keywords, entityYamlPath);
    this.stateExecutionId = stateExecutionId;
    this.accountId = accountId;
    this.delegateId = delegateId;
    this.delegateTaskId = delegateTaskId;
    this.title = title;
    this.request = request;
    this.response = response;
    this.requestTimeStamp = requestTimeStamp;
    this.responseTimeStamp = responseTimeStamp;
  }

  public ThirdPartyApiCallLog copy() {
    return ThirdPartyApiCallLog.builder()
        .stateExecutionId(stateExecutionId)
        .accountId(accountId)
        .delegateId(delegateId)
        .delegateTaskId(delegateTaskId)
        .appId(appId)
        .request(new ArrayList<>())
        .response(new ArrayList<>())
        .build();
  }

  public void addFieldToResponse(int statusCode, Object response, FieldType fieldType) {
    if (this.response == null) {
      this.response = new ArrayList<>();
    }
    String jsonResponse = fieldType == FieldType.JSON ? JsonUtils.asJson(response) : response.toString();
    this.response.add(ThirdPartyApiCallField.builder()
                          .type(FieldType.NUMBER)
                          .name(STATUS_CODE)
                          .value(Integer.toString(statusCode))
                          .build());
    this.response.add(
        ThirdPartyApiCallField.builder()
            .type(fieldType)
            .name(RESPONSE_BODY)
            .value(jsonResponse.substring(
                0, jsonResponse.length() < MAX_JSON_RESPONSE_LENGTH ? jsonResponse.length() : MAX_JSON_RESPONSE_LENGTH))
            .build());
  }

  public void addFieldToRequest(ThirdPartyApiCallField field) {
    if (request == null) {
      request = new ArrayList<>();
    }
    request.add(field);
  }

  public static ThirdPartyApiCallLog apiCallLogWithDummyStateExecution(String accountId) {
    return ThirdPartyApiCallLog.builder().accountId(accountId).stateExecutionId(NO_STATE_EXECUTION_ID).build();
  }

  @Data
  @Builder
  public static class ThirdPartyApiCallField {
    private String name;
    private String value;
    @Default private FieldType type = FieldType.TEXT;
  }

  public enum FieldType { JSON, XML, NUMBER, URL, TEXT, TIMESTAMP }

  public ExecutionStatus getStatus() {
    /*
     In an unexpected scenario where the response is an empty list of fields,
     we report it as a failed API Call
     This can only happen if the addFieldToResponse(...) method was never invoked
     on this instance.
     This is also for preventing NPE while getting 0th index of response
     */
    if (this.response.isEmpty()) {
      return ExecutionStatus.FAILED;
    }
    if (this.response.stream().anyMatch(
            obj -> obj.getName().equals(STATUS_CODE) && !obj.getValue().equals(String.valueOf(HttpStatus.SC_OK)))) {
      return ExecutionStatus.FAILED;
    }

    return ExecutionStatus.SUCCESS;
  }
}
