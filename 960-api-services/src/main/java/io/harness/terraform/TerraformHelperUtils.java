package io.harness.terraform;

import static io.harness.data.structure.HasPredicate.hasSome;

import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TerraformHelperUtils {
  public String generateCommandFlagsString(List<String> arguments, String command) {
    StringBuilder stringargs = new StringBuilder();
    if (hasSome(arguments)) {
      for (String arg : arguments) {
        stringargs.append(command).append(arg).append(' ');
      }
    }
    return stringargs.toString();
  }
}
