package software.wings.api.pcf;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PcfServiceData {
  private String name;
  private String id;
  private int previousCount;
  private int desiredCount;
}
