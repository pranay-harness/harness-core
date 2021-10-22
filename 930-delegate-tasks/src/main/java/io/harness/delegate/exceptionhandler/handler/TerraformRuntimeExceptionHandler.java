package io.harness.delegate.exceptionhandler.handler;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.task.terraform.TerraformExceptionConstants.Hints.HINT_CHECK_TERRAFORM_CONFIG_FIE;
import static io.harness.delegate.task.terraform.TerraformExceptionConstants.Hints.HINT_CHECK_TERRAFORM_CONFIG_LOCATION;
import static io.harness.delegate.task.terraform.TerraformExceptionConstants.Hints.HINT_CHECK_TERRAFORM_CONFIG_LOCATION_ARGUMENT;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.ExplanationException;
import io.harness.exception.HintException;
import io.harness.exception.TerraformCommandExecutionException;
import io.harness.exception.WingsException;
import io.harness.exception.exceptionmanager.exceptionhandler.ExceptionHandler;
import io.harness.exception.runtime.TerraformCliRuntimeException;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Singleton
@OwnedBy(CDP)
public class TerraformRuntimeExceptionHandler implements ExceptionHandler {
  private static final Pattern ERROR_PATTERN =
      Pattern.compile("\\[31m.?\\[1m.?\\[31mError:", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
  private static final Pattern ERROR_LOCATION_LINE_PATTERN = Pattern.compile("on\\s(.+?)\\sline\\s(\\d+):");
  private static final Pattern ERROR_LOCATION_LINE_BLOCK_PATTERN =
      Pattern.compile("on\\s(.+?)\\sline\\s(\\d+),\\sin\\s(.+?):\\s+\\d+:(.+)");

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

    for (String error : allErrors) {
      handleUnknownError(error, hints, explanations, structuredErrors);
    }

    if (hints.isEmpty() && explanations.isEmpty()) {
      return new TerraformCommandExecutionException(cliRuntimeException.getCliError(), WingsException.USER_SRE);
    }

    return getFinalException(explanations, hints, structuredErrors, cliRuntimeException);
  }

  private void handleUnknownError(String err, Set<String> hints, Set<String> explanations, Set<String> errorMessages) {
    Iterator<String> errorLines = Arrays.stream(err.replaceAll("\\[\\d{1,3}m", "").split("\\u001B"))
                                      .map(String::trim)
                                      .filter(EmptyPredicate::isNotEmpty)
                                      .collect(Collectors.toList())
                                      .iterator();

    if (!errorLines.hasNext()) {
      return;
    }

    errorMessages.add(errorLines.next());
    if (errorLines.hasNext()) {
      String nextError = errorLines.next();
      // check if next error line is in format "on config.tf line 1:"
      Matcher errorLocationLineMatcher = ERROR_LOCATION_LINE_PATTERN.matcher(nextError);
      if (errorLocationLineMatcher.find()) {
        String fileName = errorLocationLineMatcher.group(1);
        String line = errorLocationLineMatcher.group(2);
        // if error line matches "on config.tf line 1:" then we expect next line to be the terraform block,
        // i.e. variable "access_key"
        if (errorLines.hasNext()) {
          String terraformBlock = errorLines.next();
          hints.add(format(HINT_CHECK_TERRAFORM_CONFIG_LOCATION, fileName, line, terraformBlock));
        }
      } else {
        // otherwise check if the next line is in format "on main.tf line 1, in variable "test": argument "test""
        Matcher errorLocationLineBlockMatcher = ERROR_LOCATION_LINE_BLOCK_PATTERN.matcher(nextError);
        if (errorLocationLineBlockMatcher.find()) {
          String fileName = errorLocationLineBlockMatcher.group(1);
          String line = errorLocationLineBlockMatcher.group(2);
          String terraformBlock = errorLocationLineBlockMatcher.group(3);
          String argument = errorLocationLineBlockMatcher.group(4).trim();

          if (!terraformBlock.equalsIgnoreCase(argument)) {
            hints.add(format(HINT_CHECK_TERRAFORM_CONFIG_LOCATION_ARGUMENT, fileName, line, terraformBlock, argument));
          } else {
            hints.add(format(HINT_CHECK_TERRAFORM_CONFIG_LOCATION, fileName, line, terraformBlock));
          }
        } else {
          // if none matches then we can't identify the error location so just give a generic hint and the current
          // line (and the next ones if exists) would probably be our explanation
          hints.add(HINT_CHECK_TERRAFORM_CONFIG_FIE);
          StringBuilder explanation = new StringBuilder(nextError);
          while (errorLines.hasNext()) {
            explanation.append(' ').append(errorLines.next());
          }
          explanations.add(explanation.toString());
        }
      }

      if (errorLines.hasNext()) {
        nextError = errorLines.next();
        // in some cases the next error line will be '{', so just ignore it
        if (nextError.charAt(0) == '{') {
          nextError = errorLines.next();
        }

        StringBuilder explanation = new StringBuilder(nextError);
        while (errorLines.hasNext()) {
          explanation.append(' ').append(errorLines.next());
        }

        explanations.add(explanation.toString());
      }
    }
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
}
