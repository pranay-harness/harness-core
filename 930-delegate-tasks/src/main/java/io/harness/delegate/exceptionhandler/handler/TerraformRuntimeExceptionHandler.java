package io.harness.delegate.exceptionhandler.handler;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.NestedExceptionUtils;
import io.harness.exception.TerraformCommandExecutionException;
import io.harness.exception.WingsException;
import io.harness.exception.exceptionmanager.exceptionhandler.ExceptionHandler;
import io.harness.exception.runtime.TerraformCliRuntimeException;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.delegate.task.terraform.TerraformExceptionConstants.CliErrorMessages.NO_VALUE_FOR_REQUIRED_VARIABLE;
import static io.harness.delegate.task.terraform.TerraformExceptionConstants.Explanation.EXPLAIN_NO_VALUE_FOR_REQUIRED_VARIABLE;
import static io.harness.delegate.task.terraform.TerraformExceptionConstants.Hints.HINT_NO_VALUE_FOR_REQUIRED_VARIABLE;
import static io.harness.delegate.task.terraform.TerraformExceptionConstants.Message.MESSAGE_NO_VALUE_FOR_REQUIRED_VARIABLE;
import static java.lang.String.format;

@Singleton
@OwnedBy(CDP)
public class TerraformRuntimeExceptionHandler implements ExceptionHandler {
  private static final Pattern VARIABLE_PATTERN = Pattern.compile("input variable \"(.+?)\" is not set");

  public static Set<Class<? extends Exception>> exceptions() {
    return ImmutableSet.<Class<? extends Exception>>builder().add(TerraformCliRuntimeException.class).build();
  }

  @Override
  public WingsException handleException(Exception exception) {
    TerraformCliRuntimeException cliRuntimeException = (TerraformCliRuntimeException) exception;
    String cliErrorLowercase = cliRuntimeException.getCliError().toLowerCase();

    if (cliErrorLowercase.contains(NO_VALUE_FOR_REQUIRED_VARIABLE)) {
      String explanation = getNoVariablesRequiredExplanation(cliErrorLowercase);
      return NestedExceptionUtils.hintWithExplanationException(
              HINT_NO_VALUE_FOR_REQUIRED_VARIABLE, explanation,
              new TerraformCommandExecutionException(format(
                      MESSAGE_NO_VALUE_FOR_REQUIRED_VARIABLE, cliRuntimeException.getCommand()), WingsException.USER_SRE));
    }

    return new TerraformCommandExecutionException(cliRuntimeException.getMessage(), WingsException.SRE);
  }

  private String getNoVariablesRequiredExplanation(String errorMessage) {
    Matcher variableMatcher = VARIABLE_PATTERN.matcher(errorMessage);
    if (variableMatcher.find()) {
      StringBuilder variables = new StringBuilder(variableMatcher.group(1));
      while (variableMatcher.find()) {
        variables.append(", ").append(variableMatcher.group(1));
      }

      return EXPLAIN_NO_VALUE_FOR_REQUIRED_VARIABLE + ": " + variables;
    } else {
      return EXPLAIN_NO_VALUE_FOR_REQUIRED_VARIABLE;
    }
  }
}
