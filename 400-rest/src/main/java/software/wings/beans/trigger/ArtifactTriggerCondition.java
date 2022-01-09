/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static software.wings.beans.trigger.TriggerConditionType.NEW_ARTIFACT;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by sgurubelli on 10/25/17.
 */
@OwnedBy(CDC)
@JsonTypeName("NEW_ARTIFACT")
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "ArtifactTriggerConditionKeys")
@TargetModule(HarnessModule._815_CG_TRIGGERS)
public class ArtifactTriggerCondition extends TriggerCondition {
  @NotEmpty private String artifactStreamId;
  private String artifactSourceName;
  private String artifactFilter;
  private boolean regex;

  public ArtifactTriggerCondition() {
    super(NEW_ARTIFACT);
  }

  public ArtifactTriggerCondition(
      String artifactStreamId, String artifactSourceName, String artifactFilter, boolean regex) {
    this();
    this.artifactStreamId = artifactStreamId;
    this.artifactSourceName = artifactSourceName;
    this.artifactFilter = artifactFilter;
    this.regex = regex;
  }
}
