package io.harness.http.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.CommandExecutionStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(CDC)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HttpInternalResponse {
  private CommandExecutionStatus commandExecutionStatus;
  private String errorMessage;
  private String httpResponseBody;
  private int httpResponseCode;
  private String httpMethod;
  private String httpUrl;
  private String header;
  private boolean timedOut;
}
