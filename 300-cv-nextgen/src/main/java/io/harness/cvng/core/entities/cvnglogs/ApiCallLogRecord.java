package io.harness.cvng.core.entities.cvnglogs;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.cvnglog.ApiCallLogDTO;
import io.harness.cvng.beans.cvnglog.ApiCallLogDTO.ApiCallLogDTOField;
import io.harness.cvng.beans.cvnglog.CVNGLogDTO;

import com.google.common.base.Preconditions;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.CV)
public class ApiCallLogRecord extends CVNGLogRecord {
  private List<ApiCallLogField> requests;
  private List<ApiCallLogField> responses;
  private Instant requestTime;
  private Instant responseTime;

  public void addFieldToRequest(ApiCallLogField field) {
    Preconditions.checkNotNull(field, "Api call log request field is null.");
    if (this.requests == null) {
      this.requests = new ArrayList<>();
    }
    requests.add(field);
  }

  public void addFieldToResponse(ApiCallLogField field) {
    Preconditions.checkNotNull(field, "Api call log response field is null.");
    if (this.responses == null) {
      this.responses = new ArrayList<>();
    }
    responses.add(field);
  }

  public List<ApiCallLogField> getRequests() {
    if (requests == null) {
      return new ArrayList<>();
    }
    return requests;
  }

  public List<ApiCallLogField> getResponses() {
    if (responses == null) {
      return new ArrayList<>();
    }
    return responses;
  }

  public static CVNGLogRecord toCVNGLogRecord(CVNGLogDTO logRecordDTO) {
    ApiCallLogRecord apiCallLogRecord =
        ApiCallLogRecord.builder()
            .requestTime(Instant.ofEpochMilli(((ApiCallLogDTO) logRecordDTO).getRequestTime()))
            .responseTime(Instant.ofEpochMilli(((ApiCallLogDTO) logRecordDTO).getResponseTime()))
            .build();

    (((ApiCallLogDTO) logRecordDTO).getRequests())
        .stream()
        .map(ApiCallLogRecord::toApiCallLogField)
        .collect(Collectors.toList())
        .forEach(request -> apiCallLogRecord.addFieldToRequest(request));

    (((ApiCallLogDTO) logRecordDTO).getResponses())
        .stream()
        .map(ApiCallLogRecord::toApiCallLogField)
        .collect(Collectors.toList())
        .forEach(response -> apiCallLogRecord.addFieldToResponse(response));

    return apiCallLogRecord;
  }

  private static ApiCallLogField toApiCallLogField(ApiCallLogDTOField apiCallLogDTOField) {
    return ApiCallLogField.builder()
        .name(apiCallLogDTOField.getName())
        .type(toApiCallLogFieldType(apiCallLogDTOField.getType()))
        .value(apiCallLogDTOField.getValue())
        .build();
  }

  private static FieldType toApiCallLogFieldType(ApiCallLogDTO.FieldType dtoFieldType) {
    return FieldType.valueOf(dtoFieldType.name());
  }

  @Override
  public CVNGLogDTO toCVNGLogDTO() {
    ApiCallLogDTO apiCallLogDTO = ApiCallLogDTO.builder()
                                      .requestTime(requestTime.toEpochMilli())
                                      .responseTime(responseTime.toEpochMilli())
                                      .createdAt(getCreatedAt())
                                      .build();
    getRequests().forEach(logRecord -> apiCallLogDTO.addFieldToRequest(toApiCallLogDTOField(logRecord)));
    getResponses().forEach(logRecord -> apiCallLogDTO.addFieldToResponse(toApiCallLogDTOField(logRecord)));
    return apiCallLogDTO;
  }

  private ApiCallLogDTOField toApiCallLogDTOField(ApiCallLogField apiCallLogField) {
    return ApiCallLogDTOField.builder()
        .name(apiCallLogField.getName())
        .type(toApiCallLogDTOFieldType(apiCallLogField.getType()))
        .value(apiCallLogField.getValue())
        .build();
  }

  private ApiCallLogDTO.FieldType toApiCallLogDTOFieldType(FieldType fieldType) {
    return ApiCallLogDTO.FieldType.valueOf(fieldType.name());
  }

  @Data
  @Builder
  public static class ApiCallLogField {
    private String name;
    private String value;
    @Builder.Default private ApiCallLogRecord.FieldType type = ApiCallLogRecord.FieldType.TEXT;
  }
  public enum FieldType { JSON, XML, NUMBER, URL, TEXT, TIMESTAMP }
}
