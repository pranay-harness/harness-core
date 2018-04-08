package software.wings.helpers.ext.helm.response;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;

import java.util.List;

/**
 * Created by anubhaw on 4/2/18.
 */
@Data
public class HelmReleaseHistoryCommandResponse extends HelmCommandResponse {
  List<ReleaseInfo> releaseInfoList;

  @Builder
  public HelmReleaseHistoryCommandResponse(
      CommandExecutionStatus commandExecutionStatus, String output, List<ReleaseInfo> releaseInfoList) {
    super(commandExecutionStatus, output);
    this.releaseInfoList = releaseInfoList;
  }
}
