package software.wings.service.impl;

import static io.harness.data.encoding.EncodingUtils.compressString;
import static io.harness.data.encoding.EncodingUtils.deCompressString;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.lang.System.currentTimeMillis;
import static software.wings.service.impl.GoogleDataStoreServiceImpl.readBlob;
import static software.wings.service.impl.GoogleDataStoreServiceImpl.readLong;
import static software.wings.service.impl.GoogleDataStoreServiceImpl.readString;

import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.BlobValue;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.LongValue;
import com.google.cloud.datastore.StringValue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;
import io.harness.exception.WingsException;
import io.harness.persistence.CreatedAtAccess;
import io.harness.persistence.GoogleDataStoreAware;
import io.harness.serializer.JsonUtils;
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

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by rsingh on 1/8/18.
 */
@Entity(value = "thirdPartyApiCallLog")
@Data
@NoArgsConstructor
@ToString(exclude = {"validUntil"})
@EqualsAndHashCode(callSuper = false, exclude = {"validUntil"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class ThirdPartyApiCallLog extends Base implements GoogleDataStoreAware {
  public static final String NO_STATE_EXECUTION_ID = "NO_STATE_EXECUTION";
  private static final int MAX_JSON_RESPONSE_LENGTH = 16384;
  public static final String PAYLOAD = "Payload";
  public static final String RESPONSE_BODY = "Response Body";
  public static final String STATUS_CODE = "Status Code";

  @NotEmpty @Indexed private String stateExecutionId;
  @NotEmpty private String accountId;
  @NotEmpty private String delegateId;
  @NotEmpty private String delegateTaskId;
  private String title;
  private List<ThirdPartyApiCallField> request = new ArrayList<>();
  private List<ThirdPartyApiCallField> response = new ArrayList<>();
  private long requestTimeStamp;
  private long responseTimeStamp;

  @JsonIgnore
  @SchemaIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusWeeks(1).toInstant());

  @Builder
  public ThirdPartyApiCallLog(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, String entityYamlPath, String stateExecutionId, String accountId,
      String delegateId, String delegateTaskId, String title, List<ThirdPartyApiCallField> request,
      List<ThirdPartyApiCallField> response, long requestTimeStamp, long responseTimeStamp) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath);
    this.stateExecutionId = stateExecutionId;
    this.accountId = accountId;
    this.delegateId = delegateId;
    this.delegateTaskId = delegateTaskId;
    this.title = title;
    this.request = request;
    this.response = response;
    this.requestTimeStamp = requestTimeStamp;
    this.responseTimeStamp = responseTimeStamp;
    this.validUntil = Date.from(OffsetDateTime.now().plusMonths(1).toInstant());
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

  public static ThirdPartyApiCallLog createApiCallLog(String accountId, String appId, String stateExecutionId) {
    return ThirdPartyApiCallLog.builder()
        .accountId(accountId)
        .appId(appId)
        .stateExecutionId(isEmpty(stateExecutionId) ? NO_STATE_EXECUTION_ID : stateExecutionId)
        .build();
  }

  @Override
  public com.google.cloud.datastore.Entity convertToCloudStorageEntity(Datastore datastore) {
    Key taskKey = datastore.newKeyFactory()
                      .setKind(this.getClass().getAnnotation(org.mongodb.morphia.annotations.Entity.class).value())
                      .newKey(generateUuid());
    try {
      com.google.cloud.datastore.Entity.Builder logEntityBuilder =
          com.google.cloud.datastore.Entity.newBuilder(taskKey)
              .set("stateExecutionId", getStateExecutionId())
              .set("title", StringValue.newBuilder(getTitle()).setExcludeFromIndexes(true).build())
              .set("requestTimeStamp", LongValue.newBuilder(getRequestTimeStamp()).setExcludeFromIndexes(true).build())
              .set(
                  "responseTimeStamp", LongValue.newBuilder(getResponseTimeStamp()).setExcludeFromIndexes(true).build())
              .set("request",
                  BlobValue.newBuilder(Blob.copyFrom(compressString(JsonUtils.asJson(getRequest()))))
                      .setExcludeFromIndexes(true)
                      .build())
              .set("response",
                  BlobValue.newBuilder(Blob.copyFrom(compressString(JsonUtils.asJson(getResponse()))))
                      .setExcludeFromIndexes(true)
                      .build())
              .set(CreatedAtAccess.CREATED_AT_KEY, currentTimeMillis());
      if (isNotEmpty(getAppId())) {
        logEntityBuilder.set("appId", getAppId());
      }
      if (isNotEmpty(getAccountId())) {
        logEntityBuilder.set("accountId", StringValue.newBuilder(getAccountId()).setExcludeFromIndexes(true).build());
      }
      if (isNotEmpty(getDelegateId())) {
        logEntityBuilder.set("delegateId", StringValue.newBuilder(getDelegateId()).setExcludeFromIndexes(true).build());
      }
      if (isNotEmpty(getDelegateTaskId())) {
        logEntityBuilder.set(
            "delegateTaskId", StringValue.newBuilder(getDelegateTaskId()).setExcludeFromIndexes(true).build());
      }
      if (validUntil != null) {
        logEntityBuilder.set("validUntil", validUntil.getTime());
      }
      return logEntityBuilder.build();
    } catch (IOException e) {
      throw new WingsException(e);
    }
  }
  @Override
  public GoogleDataStoreAware readFromCloudStorageEntity(com.google.cloud.datastore.Entity entity) {
    final ThirdPartyApiCallLog apiCallLog = ThirdPartyApiCallLog.builder()
                                                .uuid(entity.getKey().getName())
                                                .stateExecutionId(readString(entity, "stateExecutionId"))
                                                .accountId(readString(entity, "accountId"))
                                                .delegateId(readString(entity, "delegateId"))
                                                .delegateTaskId(readString(entity, "delegateTaskId"))
                                                .title(readString(entity, "title"))
                                                .requestTimeStamp(readLong(entity, "requestTimeStamp"))
                                                .responseTimeStamp(readLong(entity, "responseTimeStamp"))
                                                .createdAt(readLong(entity, "createdAt"))
                                                .appId(readString(entity, "appId"))
                                                .build();
    try {
      byte[] requestBlob = readBlob(entity, "request");
      if (isNotEmpty(requestBlob)) {
        apiCallLog.setRequest(
            JsonUtils.asObject(deCompressString(requestBlob), new TypeReference<List<ThirdPartyApiCallField>>() {}));
      }
      byte[] responseBlob = readBlob(entity, "response");
      if (isNotEmpty(responseBlob)) {
        apiCallLog.setResponse(
            JsonUtils.asObject(deCompressString(responseBlob), new TypeReference<List<ThirdPartyApiCallField>>() {}));
      }
    } catch (IOException e) {
      throw new WingsException(e);
    }
    return apiCallLog;
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
