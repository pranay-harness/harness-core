package io.harness.ngmigration.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Data;

@OwnedBy(HarnessTeam.CDC)
@Data
public class NgInputDTO {
  private String accountId;
  private String orgIdentifier;
  private String projectIdentifier;
  private NgMigrationInputType inputType;
  private List<NgInputEntity> input;
}
