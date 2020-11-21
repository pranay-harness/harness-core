package io.harness.ng.core.dto;

import io.harness.ng.core.CorrelationContext;
import io.harness.ng.core.Status;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel(value = "Response")
public class ResponseDTO<T> implements SupportsEntityTag {
  Status status = Status.SUCCESS;
  T data;
  Object metaData;
  String correlationId;
  @JsonIgnore String entityTag;

  private ResponseDTO() {}

  public static ResponseDTO newResponse() {
    return newResponse(null, null, null);
  }

  public static <T> ResponseDTO<T> newResponse(T data) {
    return newResponse(null, data, null);
  }

  public static <T> ResponseDTO<T> newResponse(String entityTag, T data) {
    return newResponse(entityTag, data, null);
  }

  public static <T> ResponseDTO<T> newResponse(String entityTag, T data, Object metaData) {
    ResponseDTO<T> responseDTO = new ResponseDTO<>();
    responseDTO.setData(data);
    responseDTO.setMetaData(metaData);
    responseDTO.setCorrelationId(CorrelationContext.getCorrelationId());
    responseDTO.setStatus(Status.SUCCESS);
    responseDTO.setMetaData(metaData);
    responseDTO.setEntityTag(entityTag);
    return responseDTO;
  }
}
