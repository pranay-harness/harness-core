/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.trigger.WebhookSource.GitLabEventType;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@TargetModule(HarnessModule._815_CG_TRIGGERS)
@JsonTypeName("GITLAB")
@Value
@Builder
public class GitLabsPayloadSource implements PayloadSource {
  @NotNull private Type type = Type.GITLAB;
  private List<GitLabEventType> gitLabEventTypes;
  private List<CustomPayloadExpression> customPayloadExpressions;
}
