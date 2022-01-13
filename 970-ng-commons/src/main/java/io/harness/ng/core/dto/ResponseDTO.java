/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.CorrelationContext;
import io.harness.ng.core.Status;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@OwnedBy(PL)
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
