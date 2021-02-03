package io.harness.cvng.beans.cvnglog;

import io.harness.serializer.JsonUtils;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Preconditions;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@JsonTypeName("API_CALL_LOG")
@NoArgsConstructor
public class ApiCallLogDTO extends CVNGLogDTO {
  private static final int MAX_JSON_RESPONSE_LENGTH = 16384;
  public static final String PAYLOAD = "Payload";
  public static final String RESPONSE_BODY = "Response Body";
  public static final String STATUS_CODE = "Status Code";

  private List<ApiCallLogDTOField> requests;
  private List<ApiCallLogDTOField> responses;
  private Instant requestTime;
  private Instant responseTime;

  @Override
  public CVNGLogType getType() {
    return CVNGLogType.API_CALL_LOG;
  }

  public void addFieldToRequest(ApiCallLogDTOField field) {
    Preconditions.checkNotNull(field, "Api call log request field is null.");

    if (this.requests == null) {
      this.requests = new ArrayList<>();
    }
    requests.add(field);
  }

  public void addFieldToResponse(int statusCode, Object response, FieldType fieldType) {
    Preconditions.checkNotNull(response, "Api call log response field is null.");

    if (this.responses == null) {
      this.responses = new ArrayList<>();
    }
    String jsonResponse = getResponseToLog(response, fieldType);
    this.responses.add(ApiCallLogDTOField.builder()
                           .type(FieldType.NUMBER)
                           .name(STATUS_CODE)
                           .value(Integer.toString(statusCode))
                           .build());
    this.responses.add(
        ApiCallLogDTOField.builder()
            .type(fieldType)
            .name(RESPONSE_BODY)
            .value(jsonResponse.substring(
                0, jsonResponse.length() < MAX_JSON_RESPONSE_LENGTH ? jsonResponse.length() : MAX_JSON_RESPONSE_LENGTH))
            .build());
  }

  private String getResponseToLog(Object response, FieldType fieldType) {
    if (fieldType == null) {
      return response.toString();
    }

    switch (fieldType) {
      case JSON:
        try {
          if (response instanceof String) {
            return response.toString();
          }
          return JsonUtils.asJson(response);
        } catch (Exception e) {
          return response.toString();
        }
      default:
        return response.toString();
    }
  }

  public List<ApiCallLogDTOField> getRequests() {
    if (requests == null) {
      return new ArrayList<>();
    }
    return requests;
  }

  public List<ApiCallLogDTOField> getResponses() {
    if (responses == null) {
      return new ArrayList<>();
    }
    return responses;
  }

  @Data
  @Builder
  public static class ApiCallLogDTOField {
    private String name;
    private String value;
    @Builder.Default private ApiCallLogDTO.FieldType type = ApiCallLogDTO.FieldType.TEXT;
  }

  public enum FieldType { JSON, XML, NUMBER, URL, TEXT, TIMESTAMP }
}
