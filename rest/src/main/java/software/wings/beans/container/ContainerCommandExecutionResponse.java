package software.wings.beans.container;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.yaml.GitCommandRequest;
import software.wings.beans.yaml.GitCommandResult;
import software.wings.waitnotify.NotifyResponseData;

/**
 * Created by brett on 11/29/17.
 */
@Data
@Builder
public class ContainerCommandExecutionResponse implements NotifyResponseData {
  private GitCommandResult gitCommandResult;
  private GitCommandRequest gitCommandRequest;
  private GitCommandStatus gitCommandStatus;
  private String errorMessage;

  public enum GitCommandStatus { SUCCESS, FAILURE }
}
