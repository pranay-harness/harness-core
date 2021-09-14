/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.beans.activity.cdng;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.activity.ActivitySourceDTO;
import io.harness.cvng.beans.activity.ActivitySourceType;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonTypeName("CDNG")
@OwnedBy(HarnessTeam.CV)
public class CDNGActivitySourceDTO extends ActivitySourceDTO {
  @Override
  public ActivitySourceType getType() {
    return ActivitySourceType.CDNG;
  }

  @Override
  public boolean isEditable() {
    return false;
  }
}
