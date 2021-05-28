package io.harness.cdng.Deployment;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.AllArgsConstructor;

@OwnedBy(HarnessTeam.DX)
@AllArgsConstructor
public class TimeValuePairListDTO<T> {
  List<TimeValuePair<T>> timeValuePairList;
}
