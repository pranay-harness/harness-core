package software.wings.service.impl.yaml.handler.templatelibrary;

import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.common.TemplateConstants.SHELL_SCRIPT;

import software.wings.beans.Variable;
import software.wings.beans.VariableType;
import software.wings.beans.template.BaseTemplate;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.command.ShellScriptTemplate;

import java.util.Arrays;

public class TemplateLibaryYamlConstants {
  public static final String VALID_SHELL_SCRIPT_TEMPLATE_WITH_VARIABLE = "harnessApiVersion: '1.0'\n"
      + "type: SHELL_SCRIPT\n"
      + "outputVars: var1,var2\n"
      + "scriptString: echo asd\n"
      + "scriptType: BASH\n"
      + "timeoutMillis: 600000\n"
      + "variables:\n"
      + "- description: asd\n"
      + "  name: abc\n"
      + "  value: xyz\n";

  public static final String VALID_SHELL_SCRIPT_TEMPLATE_WITHOUT_VARIABLE = "harnessApiVersion: '1.0'\n"
      + "type: SHELL_SCRIPT\n"
      + "outputVars: var1,var2\n"
      + "scriptString: echo asd\n"
      + "scriptType: BASH\n"
      + "timeoutMillis: 600000\n";

  public static final String SHELL_SCRIPT_TEMPLATE_VALID_YAML_FILE_PATH =
      "Setup/Template Library/Harness/test-shell-script.yaml";

  public static final String INVALID_SHELL_SCRIPT_TEMPLATE_WITH_VARIABLE = "harnessApiVersion: '1.0'\n"
      + "type: SHELL_SCRIPT\n"
      + "outputVars: var1,var2\n"
      + "scriptString: echo asd\n"
      + "scriptType: BASHed\n"
      + "timeoutMillis: 600000\n"
      + "variables:\n"
      + "- description: asd\n"
      + "  name: abc\n"
      + "  value: xyz\n";
  public static final String INVALID_SHELL_SCRIPT_TEMPLATE_VALID_YAML_FILE_PATH =
      "Setup/Template Library/test-shell-script.yaml";

  public static final TemplateFolder rootTemplateFolder =
      TemplateFolder.builder().name("Harness").appId(GLOBAL_APP_ID).accountId(GLOBAL_ACCOUNT_ID).build();
  public static final Template templateForSetup = Template.builder()
                                                      .type(SHELL_SCRIPT)
                                                      .name("test-shell-script")
                                                      .accountId(GLOBAL_ACCOUNT_ID)
                                                      .appId(GLOBAL_APP_ID)
                                                      .build();
  public static final BaseTemplate baseTemplateObject = ShellScriptTemplate.builder()
                                                            .scriptString("echo asd")
                                                            .outputVars("var1,var2")
                                                            .timeoutMillis(600000)
                                                            .scriptType("BASH")
                                                            .build();
  public static final Template expectedTemplateWithoutVariable = Template.builder()
                                                                     .name("test-shell-script")
                                                                     .accountId(GLOBAL_ACCOUNT_ID)
                                                                     .appId(GLOBAL_APP_ID)
                                                                     .templateObject(baseTemplateObject)
                                                                     .build();
  private static final Variable variable = aVariable()
                                               .description("asd")
                                               .name("abc")
                                               .value("xyz")
                                               .fixed(false)
                                               .mandatory(false)
                                               .type(VariableType.TEXT)
                                               .build();
  public static final Template expectedTemplate = Template.builder()
                                                      .name("test-shell-script")
                                                      .accountId(GLOBAL_ACCOUNT_ID)
                                                      .type(SHELL_SCRIPT)
                                                      .variables(Arrays.asList(variable))
                                                      .appId(GLOBAL_APP_ID)
                                                      .templateObject(baseTemplateObject)
                                                      .build();
}
