package software.wings.helpers.ext.pcf.response;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class PcfInstanceSyncResponse extends PcfCommandResponse {
  private String name;
  private String guid;
  private String organization;
  private String space;
  private List<String> instanceIndices;

  @Builder
  public PcfInstanceSyncResponse(CommandExecutionStatus commandExecutionStatus, String output, String name, String guid,
      List<String> instanceIndicesx, String organization, String space) {
    super(commandExecutionStatus, output);
    this.name = name;
    this.guid = guid;
    this.instanceIndices = instanceIndicesx;
    this.organization = organization;
    this.space = space;
  }
}
