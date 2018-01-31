package software.wings.beans.yaml;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.ErrorCode;
import software.wings.waitnotify.NotifyResponseData;

/**
 * Created by anubhaw on 10/27/17.
 */
@Data
@Builder
public class GitCommandExecutionResponse implements NotifyResponseData {
  private GitCommandResult gitCommandResult;
  private GitCommandRequest gitCommandRequest;
  private GitCommandStatus gitCommandStatus;
  private String errorMessage;
  private ErrorCode errorCode;

  public enum GitCommandStatus { SUCCESS, FAILURE }
}
