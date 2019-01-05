package software.wings.api.ecs;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EcsBGSetupData {
  private boolean ecsBlueGreen;
  private String prodEcsListener;
  private String stageEcsListener;
  private String ecsBGTargetGroup1;
  private String ecsBGTargetGroup2;
  private String downsizedServiceName;
  private int downsizedServiceCount;
}
