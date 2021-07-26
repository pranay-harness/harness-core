package io.harness.app.beans.entities;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DashboardBuildsActiveAndFailedInfo {
  List<BuildStatusInfo> failed;
  List<BuildStatusInfo> active;
}
