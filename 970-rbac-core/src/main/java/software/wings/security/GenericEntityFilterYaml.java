package software.wings.security;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GenericEntityFilterYaml extends FilterYaml {
  private String filterType;

  @Builder
  public GenericEntityFilterYaml(List<String> names, String filterType) {
    super(names);
    this.filterType = filterType;
  }
}
