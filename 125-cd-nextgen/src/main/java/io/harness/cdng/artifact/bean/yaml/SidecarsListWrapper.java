/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.artifact.bean.yaml;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.visitor.helpers.artifact.SidecarWrapperArtifactVisitorHelper;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
@SimpleVisitorHelper(helperClass = SidecarWrapperArtifactVisitorHelper.class)
@TypeAlias("sidecarArtifactWrapper")
public class SidecarsListWrapper implements Visitable {
  List<SidecarArtifactWrapper> sidecars;

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public SidecarsListWrapper(List<SidecarArtifactWrapper> sidecars) {
    this.sidecars = sidecars;
  }
}
