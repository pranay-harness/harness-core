package software.wings.helpers.ext.helm.response;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Created by anubhaw on 4/2/18.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class HelmReleaseHistoryCommandResponse extends HelmCommandResponse {
  List<ReleaseInfo> releaseInfoList;

  @Builder
  public HelmReleaseHistoryCommandResponse(
      CommandExecutionStatus commandExecutionStatus, String output, List<ReleaseInfo> releaseInfoList) {
    super(commandExecutionStatus, output);
    this.releaseInfoList = releaseInfoList;
  }
}
