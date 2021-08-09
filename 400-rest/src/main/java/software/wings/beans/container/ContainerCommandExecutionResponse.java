package software.wings.beans.container;

import io.harness.delegate.beans.DelegateResponseData;

import software.wings.beans.yaml.GitCommandRequest;
import software.wings.beans.yaml.GitCommandResult;

import lombok.Builder;
import lombok.Data;

/**
 * Created by brett on 11/29/17.
 */
@Data
@Builder
public class ContainerCommandExecutionResponse implements DelegateResponseData {
  private GitCommandResult gitCommandResult;
  private GitCommandRequest gitCommandRequest;
  private GitCommandStatus gitCommandStatus;
  private String errorMessage;

  public enum GitCommandStatus { SUCCESS, FAILURE }
}
