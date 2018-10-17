package software.wings.beans.yaml;

import io.harness.delegate.task.protocol.ResponseData;
import io.harness.eraro.ErrorCode;
import lombok.Builder;
import lombok.Data;

/**
 * Created by anubhaw on 10/27/17.
 */
@Data
@Builder
public class GitCommandExecutionResponse implements ResponseData {
  private GitCommandResult gitCommandResult;
  private GitCommandRequest gitCommandRequest;
  private GitCommandStatus gitCommandStatus;
  private String errorMessage;
  private ErrorCode errorCode;

  public enum GitCommandStatus { SUCCESS, FAILURE }
}
