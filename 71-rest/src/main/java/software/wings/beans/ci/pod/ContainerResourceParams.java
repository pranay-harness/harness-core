package software.wings.beans.ci.pod;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContainerResourceParams {
  private Integer resourceRequestMemoryMiB;
  private Integer resourceLimitMemoryMiB;
  private Integer resourceRequestMilliCpu;
  private Integer resourceLimitMilliCpu;
}