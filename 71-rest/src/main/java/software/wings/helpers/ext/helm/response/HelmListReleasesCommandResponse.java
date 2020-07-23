package software.wings.helpers.ext.helm.response;

import io.harness.logging.CommandExecutionStatus;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Created by anubhaw on 4/2/18.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class HelmListReleasesCommandResponse extends HelmCommandResponse {
  List<ReleaseInfo> releaseInfoList;

  @Builder
  public HelmListReleasesCommandResponse(
      CommandExecutionStatus commandExecutionStatus, String output, List<ReleaseInfo> releaseInfoList) {
    super(commandExecutionStatus, output);
    this.releaseInfoList = releaseInfoList;
  }
}
