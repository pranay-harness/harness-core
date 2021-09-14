/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event.model.marketo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * @author rktummala on 02/20/19
 */
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetLeadResponse {
  //  {
  //    "requestId": "9587#1690cef64b3",
  //      "result": [
  //    {
  //      "id": 1630711,
  //        "firstName": null,
  //        "lastName": null,
  //        "email": "trk.itkid@gmail.com",
  //        "updatedAt": "2019-02-20T21:55:01Z",
  //        "createdAt": "2019-02-20T21:46:17Z"
  //    }
  //    ],
  //    "success": true
  //  }

  private String requestId;
  private List<Result> result;
  private List<Error> errors;
  private boolean success;

  @Data
  @Builder
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Result {
    private int id;
    private String firstName;
    private String lastName;
    private String email;
  }
}
