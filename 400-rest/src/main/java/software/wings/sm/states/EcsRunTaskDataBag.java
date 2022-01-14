package software.wings.sm.states;

import software.wings.beans.AwsConfig;

import java.util.List;
import lombok.Builder;
import lombok.Data;

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
