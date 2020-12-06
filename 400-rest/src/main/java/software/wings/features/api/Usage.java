package software.wings.features.api;

import java.util.Map;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.ToString;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
@ToString
public class Usage {
  @NonNull @NotEmpty String entityId;
  @NonNull @NotEmpty String entityType;
  @NonNull @NotEmpty String entityName;
  @Singular Map<String, String> properties;
}
