package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@OwnedBy(CDC)
@Getter
@Setter
@Builder
public class ParallelInfo {
  private int groupIndex;
}
