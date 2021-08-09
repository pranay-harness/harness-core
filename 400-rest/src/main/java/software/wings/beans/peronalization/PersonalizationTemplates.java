package software.wings.beans.peronalization;

import java.util.Set;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "PersonalizationTemplatesKeys")
public class PersonalizationTemplates {
  private Set<String> favorites;
}
