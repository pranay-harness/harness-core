package software.wings.graphql.datafetcher;

import io.harness.exception.InvalidRequestException;
import lombok.experimental.UtilityClass;
import software.wings.beans.EntityType;
import software.wings.beans.Variable;
import software.wings.beans.VariableDisplayType;
import software.wings.beans.VariableType;
import software.wings.graphql.schema.type.QLVariable;

import java.util.List;

@UtilityClass
public class VariableController {
  public static void populateVariables(List<Variable> userVariables, List<QLVariable> qlVariables) {
    if (userVariables == null) {
      return;
    }
    userVariables.stream()
        .map(variable
            -> QLVariable.builder()
                   .name(variable.getName())
                   .type(getStringType(variable.getType(), variable.obtainEntityType(), variable.getName()))
                   .required(variable.isMandatory())
                   .description(variable.getDescription())
                   .allowedValues(variable.getAllowedList())
                   .defaultValue(variable.getValue())
                   .fixed(variable.isFixed())
                   .build())
        .forEach(qlVariables::add);
  }

  private static String getStringType(VariableType type, EntityType entityType, String name) {
    if (type == VariableType.ENTITY) {
      if (entityType == null) {
        throw new InvalidRequestException("Entity type should not be null for Entity variable: " + name);
      }
      return VariableDisplayType.valueOf(entityType.name()).getDisplayName();
    }

    return VariableDisplayType.TEXT.getDisplayName();
  }
}
