package software.wings.sm.states;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.AwsConfig;

import java.util.List;

@Data
@Builder
public class EcsRunTaskDataBag {
  AwsConfig awsConfig;
  String envUuid;
  String applicationAccountId;
  String applicationAppId;
  String applicationUuid;
  String ecsRunTaskFamilyName;
  List<String> listTaskDefinitionJson;
  boolean skipSteadyStateCheck;
  Long serviceSteadyStateTimeout;
}
