package software.wings.service.impl.security.customsecretsmanager;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;

import com.google.common.collect.Sets;

import io.harness.exception.InvalidArgumentsException;
import io.harness.security.encryption.SecretVariable;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerConfig;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@UtilityClass
class CustomSecretsManagerValidationUtils {
  void validateName(@NotEmpty String name) {
    Pattern nameValidator = Pattern.compile("^[0-9a-zA-Z-' !_]+$");

    if (isEmpty(name) || !nameValidator.matcher(name).find()) {
      String message =
          "Name cannot be empty and can only have alphanumeric, hyphen, single inverted comma, space and exclamation mark characters.";
      throw new InvalidArgumentsException(message, USER);
    }
  }

  void validateVariables(
      @NonNull CustomSecretsManagerConfig customSecretsManagerConfig, @NonNull Set<SecretVariable> testVariables) {
    Set<String> shellScriptVariables =
        new HashSet<>(customSecretsManagerConfig.getCustomSecretsManagerShellScript().getVariables());
    Set<String> receivedVariables = testVariables.stream().map(SecretVariable::getName).collect(Collectors.toSet());
    Set<String> diff = Sets.difference(shellScriptVariables, receivedVariables);
    if (!diff.isEmpty()) {
      String message = String.format(
          "The values for the variables %s have not been provided as part of test parameters", String.join(", ", diff));
      throw new InvalidArgumentsException(message, USER);
    }
  }

  void validateConnectionAttributes(@NonNull CustomSecretsManagerConfig customSecretsManagerConfig) {
    if (isEmpty(customSecretsManagerConfig.getCommandPath())) {
      String message = "Command path for the custom secret manager cannot be empty";
      throw new InvalidArgumentsException(message, USER);
    }

    if (!customSecretsManagerConfig.isExecuteOnDelegate()) {
      if (isEmpty(customSecretsManagerConfig.getHost())) {
        String message = "Target host cannot be empty when the secret has to be retrieved from another system.";
        throw new InvalidArgumentsException(message, USER);
      }
    }
  }
}
