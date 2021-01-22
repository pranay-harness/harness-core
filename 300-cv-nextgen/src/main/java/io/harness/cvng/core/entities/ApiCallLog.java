package io.harness.cvng.core.entities;

import io.harness.cvng.beans.cvnglog.ApiCallLogDTO;
import io.harness.cvng.beans.cvnglog.ApiCallLogDTO.ApiCallLogDTOField;
import io.harness.cvng.beans.cvnglog.CVNGLogDTO;
import io.harness.cvng.beans.cvnglog.CVNGLogType;

import com.google.common.base.Preconditions;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@FieldNameConstants(innerTypeName = "ApiCallLogKeys")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ApiCallLog extends CVNGLog {
  private List<ApiCallLogField> requests;
  private List<ApiCallLogField> responses;
  private Instant requestTime;
  private Instant responseTime;

  @Data
  @Builder
  public static class ApiCallLogField {
    private String name;
    private String value;
    @Builder.Default private ApiCallLog.FieldType type = ApiCallLog.FieldType.TEXT;
  }

  @Override
  public CVNGLogType getType() {
    return CVNGLogType.API_CALL_LOG;
  }

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

  public enum FieldType { JSON, XML, NUMBER, URL, TEXT, TIMESTAMP }

  public static CVNGLog toApiCallLog(CVNGLogDTO logRecordDTO) {
    CVNGLog apiCallLog = ApiCallLog.builder()
                             .accountId(logRecordDTO.getAccountId())
                             .traceableId(logRecordDTO.getTraceableId())
                             .responseTime(((ApiCallLogDTO) logRecordDTO).getResponseTime())
                             .requestTime(((ApiCallLogDTO) logRecordDTO).getRequestTime())
                             .startTime(logRecordDTO.getStartTime())
                             .endTime(logRecordDTO.getEndTime())
                             .traceableType(logRecordDTO.getTraceableType())
                             .createdAt(logRecordDTO.getCreatedAt())
                             .build();

    (((ApiCallLogDTO) logRecordDTO).getRequests())
        .stream()
        .map(ApiCallLog::toApiCallLogField)
        .collect(Collectors.toList())
        .forEach(request -> ((ApiCallLog) apiCallLog).addFieldToRequest(request));

    (((ApiCallLogDTO) logRecordDTO).getResponses())
        .stream()
        .map(ApiCallLog::toApiCallLogField)
        .collect(Collectors.toList())
        .forEach(response -> ((ApiCallLog) apiCallLog).addFieldToResponse(response));

    return apiCallLog;
  }

  private static ApiCallLogField toApiCallLogField(ApiCallLogDTOField apiCallLogDTOField) {
    return ApiCallLogField.builder()
        .name(apiCallLogDTOField.getName())
        .type(toApiCallLogFieldType(apiCallLogDTOField.getType()))
        .value(apiCallLogDTOField.getValue())
        .build();
  }

  private static ApiCallLog.FieldType toApiCallLogFieldType(ApiCallLogDTO.FieldType dtoFieldType) {
    return ApiCallLog.FieldType.valueOf(dtoFieldType.name());
  }
}
