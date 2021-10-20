package io.harness.delegate.exceptionhandler.handler;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.task.terraform.TerraformExceptionConstants.CliErrorMessages.NO_VALUE_FOR_REQUIRED_VARIABLE;
import static io.harness.delegate.task.terraform.TerraformExceptionConstants.Explanation.EXPLAIN_NO_VALUE_FOR_REQUIRED_VARIABLE;
import static io.harness.delegate.task.terraform.TerraformExceptionConstants.Hints.HINT_NO_VALUE_FOR_REQUIRED_VARIABLE;
import static io.harness.delegate.task.terraform.TerraformExceptionConstants.Message.MESSAGE_NO_VALUE_FOR_REQUIRED_VARIABLE;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.ExplanationException;
import io.harness.exception.HintException;
import io.harness.exception.TerraformCommandExecutionException;
import io.harness.exception.WingsException;
import io.harness.exception.exceptionmanager.exceptionhandler.ExceptionHandler;
import io.harness.exception.runtime.TerraformCliRuntimeException;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Singleton
@OwnedBy(CDP)
public class TerraformRuntimeExceptionHandler implements ExceptionHandler {
  private static final Pattern ERROR_PATTERN =
      Pattern.compile("\\[31m.?\\[1m.?\\[31mError:", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
  private static final Pattern VARIABLE_PATTERN = Pattern.compile("input variable \"(.+?)\" is not set");

  public static Set<Class<? extends Exception>> exceptions() {
    return ImmutableSet.<Class<? extends Exception>>builder().add(TerraformCliRuntimeException.class).build();
  }

  @Override
  public WingsException handleException(Exception exception) {
    TerraformCliRuntimeException cliRuntimeException = (TerraformCliRuntimeException) exception;
    Set<String> allErrors = getAllErrors(cliRuntimeException.getCliError());
    Set<String> explanations = new HashSet<>();
    Set<String> hints = new HashSet<>();
    Set<String> structuredErrors = new HashSet<>();

    Set<String> filteredErrors = filterErrors(allErrors, error -> error.contains(NO_VALUE_FOR_REQUIRED_VARIABLE));
    if (isNotEmpty(filteredErrors)) {
      StringBuilder variablesBuilder = new StringBuilder();
      for (String error : filteredErrors) {
        Matcher variableMatcher = VARIABLE_PATTERN.matcher(error);
        if (variableMatcher.find()) {
          if (isNotEmpty(variablesBuilder.toString())) {
            variablesBuilder.append(", ");
          }
          variablesBuilder.append(variableMatcher.group(1));
        }
      }

      String variables = variablesBuilder.toString();
      String explanation = EXPLAIN_NO_VALUE_FOR_REQUIRED_VARIABLE;
      if (isNotEmpty(variables)) {
        explanation += ": " + variables;
      }

      explanations.add(explanation);
      hints.add(HINT_NO_VALUE_FOR_REQUIRED_VARIABLE);
      structuredErrors.add(MESSAGE_NO_VALUE_FOR_REQUIRED_VARIABLE);
      allErrors.removeAll(filteredErrors);
    }

    if (hints.isEmpty() && explanations.isEmpty()) {
      return new TerraformCommandExecutionException(cliRuntimeException.getCliError(), WingsException.USER_SRE);
    }

    return getFinalException(explanations, hints, structuredErrors, cliRuntimeException);
  }

  private WingsException getFinalException(Set<String> explanations, Set<String> hints, Set<String> structuredErrors,
      TerraformCliRuntimeException cliRuntimeException) {
    TerraformCommandExecutionException terraformCommandException;
    if (structuredErrors.size() == 1) {
      terraformCommandException = new TerraformCommandExecutionException(
          format("%s failed with: %s", cliRuntimeException.getCommand(), structuredErrors.iterator().next()),
          WingsException.USER_SRE);
    } else if (!isNotEmpty(structuredErrors)) {
      terraformCommandException = new TerraformCommandExecutionException(
          format("%s failed with: '%s' and (%d) more errors", cliRuntimeException.getCommand(),
              structuredErrors.iterator().next(), structuredErrors.size() - 1),
          WingsException.USER_SRE);
    } else {
      terraformCommandException = new TerraformCommandExecutionException(
          format("%s failed", cliRuntimeException.getCommand()), WingsException.USER_SRE);
    }

    Iterator<String> hintsIterator = hints.iterator();
    Iterator<String> explanationsIterator = explanations.iterator();

    WingsException latestException = terraformCommandException;
    while (hintsIterator.hasNext()) {
      WingsException hintCause = explanationsIterator.hasNext()
          ? new ExplanationException(explanationsIterator.next(), latestException)
          : latestException;
      latestException = new HintException(hintsIterator.next(), hintCause);
    }

    while (explanationsIterator.hasNext()) {
      latestException = new ExplanationException(explanationsIterator.next(), latestException);
    }

    return latestException;
  }

  @VisibleForTesting
  static Set<String> getAllErrors(String unstructuredError) {
    Matcher errorMatcher = ERROR_PATTERN.matcher(unstructuredError);
    Set<String> allErrors = new HashSet<>();
    int errorMessageStartAt;
    int errorMessageEndAt;

    if (!errorMatcher.find()) {
      return allErrors;
    }

    errorMessageStartAt = errorMatcher.end();
    while (errorMatcher.find()) {
      errorMessageEndAt = errorMatcher.start();
      allErrors.add(unstructuredError.substring(errorMessageStartAt, errorMessageEndAt).trim());
      errorMessageStartAt = errorMatcher.end();
    }

    allErrors.add(unstructuredError.substring(errorMessageStartAt).trim());

    return allErrors;
  }

  private static Set<String> filterErrors(Set<String> errors, Predicate<? super String> predicate) {
    return errors.stream().map(String::toLowerCase).filter(predicate).collect(Collectors.toSet());
  }
}
