package software.wings.beans.peronalization;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

import java.util.Set;

@Value
@Builder
@FieldNameConstants(innerTypeName = "PersonalizationTemplatesKeys")
public class PersonalizationTemplates {
  private Set<String> favorites;
}
