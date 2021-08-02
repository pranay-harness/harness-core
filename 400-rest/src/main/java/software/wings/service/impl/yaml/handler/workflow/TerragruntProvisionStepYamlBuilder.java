package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.sm.states.provision.TerragruntProvisionState.TerragruntProvisionStateKeys;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
@Slf4j
@OwnedBy(CDP)
public class TerragruntProvisionStepYamlBuilder extends InfraProvisionStepYamlBuilder {
  private static final List<String> ENCRYPTED_PROPERTIES = Arrays.asList(TerragruntProvisionStateKeys.backendConfigs,
      TerragruntProvisionStateKeys.environmentVariables, TerragruntProvisionStateKeys.variables);

  @Override
  public void convertIdToNameForKnownTypes(String name, Object objectValue, Map<String, Object> outputProperties,
      String appId, Map<String, Object> inputProperties) {
    if (ENCRYPTED_PROPERTIES.contains(name)) {
      convertPropertyIdsToNames(name, appId, objectValue);
    }

    outputProperties.put(name, objectValue);
  }

  @Override
  public void convertNameToIdForKnownTypes(String name, Object objectValue, Map<String, Object> outputProperties,
      String appId, String accountId, Map<String, Object> inputProperties) {
    if (ENCRYPTED_PROPERTIES.contains(name)) {
      convertPropertyNamesToIds(name, accountId, objectValue);
    }

    outputProperties.put(name, objectValue);
  }
}
