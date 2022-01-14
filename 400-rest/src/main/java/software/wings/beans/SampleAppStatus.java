package software.wings.beans;

import software.wings.beans.SampleAppEntityStatus.Health;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SampleAppStatus {
  String deploymentType;
  private Health health;
  List<SampleAppEntityStatus> statusList;
}
