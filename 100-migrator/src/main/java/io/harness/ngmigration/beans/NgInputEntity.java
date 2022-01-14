package io.harness.ngmigration.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.ngmigration.CgEntityId;

import lombok.Data;

@OwnedBy(HarnessTeam.CDC)
@Data
public class NgInputEntity {
  private CgEntityId cgEntityId;
  private String orgIdentifier;
  private String projectIdentifier;
}
