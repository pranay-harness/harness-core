package software.wings.service.impl.instance;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FlatEntitySummaryStats {
  private String entityId;
  private String entityName;
  private String entityVersion;
  private int count;
}
