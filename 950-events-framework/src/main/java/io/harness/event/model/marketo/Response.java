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
