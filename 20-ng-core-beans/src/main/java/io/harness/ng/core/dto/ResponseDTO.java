package io.harness.ng.core.dto;

import io.harness.ng.core.CorrelationContext;
import io.harness.ng.core.Status;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResponseDTO<T> {
  Status status = Status.SUCCESS;
  T data;
  Object metaData;
  String correlationId;

  private ResponseDTO() {}

  public static ResponseDTO newResponse() {
    return newResponse(null, null);
  }

  public static <T> ResponseDTO<T> newResponse(T data) {
    return newResponse(data, null);
  }

  public static <T> ResponseDTO<T> newResponse(T data, Object metaData) {
    ResponseDTO<T> responseDTO = new ResponseDTO<>();
    responseDTO.setData(data);
    responseDTO.setMetaData(metaData);
    responseDTO.setCorrelationId(CorrelationContext.getCorrelationId());
    responseDTO.setStatus(Status.SUCCESS);
    responseDTO.setMetaData(metaData);
    return responseDTO;
  }
}
