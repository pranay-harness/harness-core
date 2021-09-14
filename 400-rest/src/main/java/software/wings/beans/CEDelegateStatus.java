/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.DelegateConnectionDetails;
import io.harness.delegate.beans.DelegateInstanceStatus;
import io.harness.k8s.model.response.CEK8sDelegatePrerequisite;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CE)
@Value
@Builder(toBuilder = true)
@TargetModule(HarnessModule._920_DELEGATE_SERVICE_BEANS)
public class CEDelegateStatus {
  private String uuid;
  private Boolean found;
  private Boolean ceEnabled;
  private Long lastHeartBeat;
  private String delegateName;
  private String delegateType;
  private DelegateInstanceStatus status;
  @Builder.Default private List<DelegateConnectionDetails> connections = new ArrayList<>();
  private CEK8sDelegatePrerequisite.MetricsServerCheck metricsServerCheck;
  private List<CEK8sDelegatePrerequisite.Rule> permissionRuleList;
}
