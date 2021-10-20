package io.harness.delegate.exceptionhandler.handler;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.delegate.task.terraform.TerraformExceptionConstants.Explanation.EXPLAIN_NO_VALUE_FOR_REQUIRED_VARIABLE;
import static io.harness.delegate.task.terraform.TerraformExceptionConstants.Hints.HINT_NO_VALUE_FOR_REQUIRED_VARIABLE;
import static io.harness.delegate.task.terraform.TerraformExceptionConstants.Message.MESSAGE_NO_VALUE_FOR_REQUIRED_VARIABLE;
import static io.harness.rule.OwnerRule.ABOSII;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.ExplanationException;
import io.harness.exception.HintException;
import io.harness.exception.TerraformCommandExecutionException;
import io.harness.exception.WingsException;
import io.harness.exception.runtime.TerraformCliRuntimeException;
import io.harness.rule.Owner;

import java.util.Set;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDP)
public class TerraformRuntimeExceptionHandlerTest {
  private static final String TEST_ERROR_SAMPLE1 =
      "\u001B[31m\u001B[1m\u001B[31mError: \u001B[0m\u001B[0m\u001B[1mUnreadable module directory\u001B[0m\u001B[0mUnable to evaluate directory symlink: lstat modues: no such file or directory\u001B[0m\u001B[0m\u001B[31m\u001B[1m\u001B[31mError: \u001B[0m\u001B[0m\u001B[1mFailed to read module directory\u001B[0m\u001B[0mModule directory  does not exist or cannot be read.\u001B[0m\u001B[0m\u001B[31m\u001B[1m\u001B[31mError: \u001B[0m\u001B[0m\u001B[1mUnreadable module directory\u001B[0m\u001B[0mUnable to evaluate directory symlink: lstat modues: no such file or directory\u001B[0m\u001B[0m\u001B[31m\u001B[1m\u001B[31mError: \u001B[0m\u001B[0m\u001B[1mFailed to read module directory\u001B[0m\u001B[0mModule directory  does not exist or cannot be read.\u001B[0m\u001B[0m\u001B[31m\u001B[1m\u001B[31mError: \u001B[0m\u001B[0m\u001B[1mUnreadable module directory\u001B[0m\u001B[0mUnable to evaluate directory symlink: lstat modues: no such file or directory\u001B[0m\u001B[0m\u001B[31m\u001B[1m\u001B[31mError: \u001B[0m\u001B[0m\u001B[1mFailed to read module directory\u001B[0m\u001B[0mModule directory  does not exist or cannot be read.\u001B[0m\u001B[0m";
  private static final String TEST_ERROR_NO_VALUE_VARIABLE =
      "\u001B[31m\u001B[1m\u001B[31mError: \u001B[0m\u001B[0m\u001B[1mNo value for required variable\u001B[0m\u001B[0m  on config.tf line 1:   1: \u001B[4mvariable \"access_key\"\u001B[0m {\u001B[0mThe root module input variable \"access_key\" is not set, and has no defaultvalue. Use a -var or -var-file command line argument to provide a value forthis variable.\u001B[0m\u001B[0m\u001B[31m\u001B[1m\u001B[31mError: \u001B[0m\u001B[0m\u001B[1mNo value for required variable\u001B[0m\u001B[0m  on config.tf line 4:   4: \u001B[4mvariable \"secret_key\"\u001B[0m {\u001B[0mThe root module input variable \"secret_key\" is not set, and has no defaultvalue. Use a -var or -var-file command line argument to provide a value forthis variable.\u001B[0m\u001B[0m\u001B[31m\u001B[1m\u001B[31mError: \u001B[0m\u001B[0m\u001B[1mNo value for required variable\u001B[0m\u001B[0m  on config.tf line 7:   7: \u001B[4mvariable \"region\"\u001B[0m {\u001B[0mThe root module input variable \"region\" is not set, and has no default value.Use a -var or -var-file command line argument to provide a value for thisvariable.\u001B[0m\u001B[0m\u001B[31m\u001B[1m\u001B[31mError: \u001B[0m\u001B[0m\u001B[1mNo value for required variable\u001B[0m\u001B[0m  on config.tf line 11:  11: \u001B[4mvariable \"tag_name\"\u001B[0m {\u001B[0mThe root module input variable \"tag_name\" is not set, and has no defaultvalue. Use a -var or -var-file command line argument to provide a value forthis variable.\u001B[0m\u001B[0m\u001B[31m\u001B[1m\u001B[31mError: \u001B[0m\u001B[0m\u001B[1mNo value for required variable\u001B[0m\u001B[0m  on config.tf line 16:  16: \u001B[4mvariable \"keyName\"\u001B[0m {}\u001B[0mThe root module input variable \"keyName\" is not set, and has no default value.Use a -var or -var-file command line argument to provide a value for thisvariable.\u001B[0m\u001B[0m";

  TerraformRuntimeExceptionHandler handler = new TerraformRuntimeExceptionHandler();

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetAllErrorsSample1() {
    Set<String> errors = TerraformRuntimeExceptionHandler.getAllErrors(TEST_ERROR_SAMPLE1);
    // only 2 unique errors
    assertThat(errors).hasSize(2);
    assertThat(errors).containsExactlyInAnyOrder(
        "[0m\u001B[0m\u001B[1mUnreadable module directory\u001B[0m\u001B[0mUnable to evaluate directory symlink: lstat modues: no such file or directory\u001B[0m\u001B[0m",
        "[0m\u001B[0m\u001B[1mFailed to read module directory\u001B[0m\u001B[0mModule directory  does not exist or cannot be read.\u001B[0m\u001B[0m");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testHandleExceptionNoValueForRequiredVariable() {
    TerraformCliRuntimeException cliRuntimeException =
        new TerraformCliRuntimeException("Terraform failed", "terraform refresh", TEST_ERROR_NO_VALUE_VARIABLE);
    WingsException handledException = handler.handleException(cliRuntimeException);
    HintException hint = ExceptionUtils.cause(HintException.class, handledException);
    ExplanationException explanation = ExceptionUtils.cause(ExplanationException.class, handledException);
    TerraformCommandExecutionException exception =
        ExceptionUtils.cause(TerraformCommandExecutionException.class, handledException);

    assertThat(hint.getMessage()).isEqualTo(HINT_NO_VALUE_FOR_REQUIRED_VARIABLE);
    assertThat(explanation.getMessage())
        .isEqualTo(EXPLAIN_NO_VALUE_FOR_REQUIRED_VARIABLE + ": secret_key, region, tag_name, keyname, access_key");
    assertThat(exception.getMessage()).contains(MESSAGE_NO_VALUE_FOR_REQUIRED_VARIABLE);
  }
}