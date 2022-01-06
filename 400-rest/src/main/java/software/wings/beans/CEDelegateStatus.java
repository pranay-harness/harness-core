/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
