package software.wings.api.instancedetails;

import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Builder
public class InstanceApiResponse {
  private List<String> instances;
  @Getter(AccessLevel.NONE) private Integer newInstanceTrafficPercent;
  private boolean skipVerification;

  public Optional<Integer> getNewInstanceTrafficPercent() {
    return Optional.ofNullable(newInstanceTrafficPercent);
  }
}
