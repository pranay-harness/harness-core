package software.wings.beans;

import io.harness.data.validator.Trimmed;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
public class BlueprintProperty {
  @NotEmpty @Trimmed private String name;
  @NotNull private String value;
  private String valueType;
  private List<NameValuePair> fields;

  @Data
  @Builder
  public static final class Yaml {
    @NotEmpty @Trimmed private String name;
    @NotNull private String value;
    private String valueType;
    private List<NameValuePair.Yaml> fields;
  }
}
