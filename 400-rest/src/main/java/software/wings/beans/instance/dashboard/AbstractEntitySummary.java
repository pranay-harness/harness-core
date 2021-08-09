package software.wings.beans.instance.dashboard;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public abstract class AbstractEntitySummary {
  private String id;
  private String name;
  private String type;

  public AbstractEntitySummary(String id, String name, String type) {
    this.id = id;
    this.name = name;
    this.type = type;
  }
}
