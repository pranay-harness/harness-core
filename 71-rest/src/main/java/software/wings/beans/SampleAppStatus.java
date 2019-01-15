package software.wings.beans;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.SampleAppEntityStatus.Health;

import java.util.List;

@Data
@Builder
public class SampleAppStatus {
  String deploymentType;
  private Health health;
  List<SampleAppEntityStatus> statusList;
}
