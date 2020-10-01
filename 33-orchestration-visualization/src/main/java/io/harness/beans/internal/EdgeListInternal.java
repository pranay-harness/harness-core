package io.harness.beans.internal;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.util.List;

@OwnedBy(CDC)
@Value
@Builder
public class EdgeListInternal {
  @NonFinal @Setter String parentId;
  List<String> prevIds;
  List<String> nextIds;

  List<String> edges;
}
