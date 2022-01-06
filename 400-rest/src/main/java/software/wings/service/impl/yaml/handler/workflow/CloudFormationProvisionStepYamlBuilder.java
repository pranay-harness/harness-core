/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidArgumentsException;

import software.wings.beans.yaml.ChangeContext;
import software.wings.sm.states.provision.CloudFormationState.CloudFormationStateKeys;
import software.wings.yaml.workflow.StepYaml;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
@Slf4j
@OwnedBy(CDP)
public class CloudFormationProvisionStepYamlBuilder extends InfraProvisionStepYamlBuilder {
  @Override
  public void validate(ChangeContext<StepYaml> changeContext) {
    super.validate(changeContext);

    StepYaml cloudFormationStepYaml = changeContext.getYaml();
    Map<String, Object> cloudFormationStepPropertiesMap = cloudFormationStepYaml.getProperties();
    List<String> stackStatusesToMarkAsSuccess =
        (List) cloudFormationStepPropertiesMap.get("stackStatusesToMarkAsSuccess");
    Boolean skipBasedOnStackStatus = (Boolean) cloudFormationStepPropertiesMap.get("skipBasedOnStackStatus");
    if (skipBasedOnStackStatus != null) {
      if (skipBasedOnStackStatus == Boolean.TRUE) {
        if (EmptyPredicate.isEmpty(stackStatusesToMarkAsSuccess)) {
          throw new InvalidArgumentsException(
              "Provided CloudFormation Step Yaml is Invalid: skipBasedOnStackStatus is true, but the list stackStatusesToMarkAsSuccess is empty");
        }
      } else {
        if (EmptyPredicate.isNotEmpty(stackStatusesToMarkAsSuccess)) {
          throw new InvalidArgumentsException(
              "Provided CloudFormation Step Yaml is Invalid: skipBasedOnStackStatus is false, but the list stackStatusesToMarkAsSuccess is not empty");
        }
      }
    } else {
      if (EmptyPredicate.isNotEmpty(stackStatusesToMarkAsSuccess)) {
        throw new InvalidArgumentsException(
            "Provided CloudFormation Step Yaml is Invalid: skipBasedOnStackStatus is null, but the list stackStatusesToMarkAsSuccess is not empty");
      }
    }
  }

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
