package software.wings.beans.peronalization;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

import java.util.LinkedList;
import java.util.Set;

@Value
@Builder
@FieldNameConstants(innerTypeName = "PersonalizationStepsKeys")
public class PersonalizationSteps {
  private Set<String> favorites;
  private LinkedList<String> recent;
}
