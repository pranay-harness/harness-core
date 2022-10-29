/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.api.cloudformation;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.context.ContextElementType;

import software.wings.sm.ContextElement;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(CDP)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class CloudFormationRollbackInfoElement implements CloudFormationElement {
  private String awsConfigId;
  private String region;
  private boolean stackExisted;
  private String oldStackBody;
  private String stackNameSuffix;
  private String customStackName;
  private String provisionerId;
  private boolean skipBasedOnStackStatus;
  private List<String> stackStatusesToMarkAsSuccess;
  private Map<String, String> oldStackParameters;
  private List<String> capabilities;
  private String tags;

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.CLOUD_FORMATION_ROLLBACK;
  }

  @Override
  public String getUuid() {
    return null;
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public ContextElement cloneMin() {
    return null;
  }
}
