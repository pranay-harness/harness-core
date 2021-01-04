package software.wings.beans;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SampleAppEntityStatus {
  private String entityName;
  private String entityType;
  private Health health;
  public enum Health { GOOD, BAD }
  ;
}
