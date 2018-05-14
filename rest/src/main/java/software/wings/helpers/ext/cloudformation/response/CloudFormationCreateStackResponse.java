package software.wings.helpers.ext.cloudformation.response;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;

import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
public class CloudFormationCreateStackResponse extends CloudFormationCommandResponse {
  String stackId;
  String region;
  List<String> vpcs;
  List<String> subnets;
  List<String> securityGroups;
  Map<String, Object> tagMap;

  @Builder
  public CloudFormationCreateStackResponse(CommandExecutionStatus commandExecutionStatus, String output, String stackId,
      String region, List<String> vpcs, List<String> subnets, List<String> securityGroups, Map<String, Object> tagMap) {
    super(commandExecutionStatus, output);
    this.stackId = stackId;
    this.region = region;
    this.vpcs = vpcs;
    this.subnets = subnets;
    this.securityGroups = securityGroups;
    this.tagMap = tagMap;
  }
}