package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.sm.states.provision.CloudFormationState.CloudFormationStateKeys;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
@Slf4j
@OwnedBy(CDP)
public class CloudFormationStepYamlBuilder extends InfraProvisionStepYamlBuilder {
  @Override
  public void convertIdToNameForKnownTypes(String name, Object objectValue, Map<String, Object> outputProperties,
      String appId, Map<String, Object> inputProperties) {
    if (CloudFormationStateKeys.variables.equals(name)) {
      convertPropertyIdsToNames(name, appId, objectValue);
    } else if (CloudFormationStateKeys.provisionerId.equals(name)) {
      objectValue = convertProvisionerIdToName(appId, objectValue);
      name = PROVISIONER_NAME;
    }

    outputProperties.put(name, objectValue);
  }

  @Override
  public void convertNameToIdForKnownTypes(String name, Object objectValue, Map<String, Object> outputProperties,
      String appId, String accountId, Map<String, Object> inputProperties) {
    if (CloudFormationStateKeys.variables.equals(name)) {
      convertPropertyNamesToIds(name, accountId, objectValue);
    } else if (PROVISIONER_NAME.equals(name)) {
      objectValue = convertProvisionerNameToId(appId, objectValue);
      name = CloudFormationStateKeys.provisionerId;
    }

    outputProperties.put(name, objectValue);
  }
}
