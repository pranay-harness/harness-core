package software.wings.beans;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TrafficShiftMetadata {
  private List<String> phaseIdsWithTrafficShift;
}
