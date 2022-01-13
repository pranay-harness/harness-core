/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.event.model.marketo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * @author rktummala
 */

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Response {
  private String requestId;
  private List<Result> result;
  private List<Error> errors;
  private boolean success;

  @Data
  @Builder
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Result {
    private String status;
    private long id;
    private List<Error> reasons;
  }
}
