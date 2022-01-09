/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.yaml.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.trigger.CustomPayloadExpression;
import software.wings.beans.trigger.PayloadSource.Type;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
@OwnedBy(CDC)
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("BITBUCKET")
@JsonPropertyOrder({"harnessApiVersion"})
@TargetModule(HarnessModule._815_CG_TRIGGERS)
public class GitlabPayloadSourceYaml extends PayloadSourceYaml {
  private List<WebhookEventYaml> events;
  private List<CustomPayloadExpression> customPayloadExpressions;

  GitlabPayloadSourceYaml() {
    super.setType(Type.GITLAB.name());
  }

  @Builder
  GitlabPayloadSourceYaml(List<WebhookEventYaml> events, List<CustomPayloadExpression> customPayloadExpressions) {
    super.setType(Type.GITLAB.name());
    this.events = events;
    this.customPayloadExpressions = customPayloadExpressions;
  }
}
