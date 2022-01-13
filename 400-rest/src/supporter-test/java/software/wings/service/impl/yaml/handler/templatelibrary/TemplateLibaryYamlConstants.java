/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.templatelibrary;

import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.CGConstants.GLOBAL_APP_ID;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.common.TemplateConstants.ARTIFACT_SOURCE;
import static software.wings.common.TemplateConstants.HTTP;
import static software.wings.common.TemplateConstants.PCF_PLUGIN;
import static software.wings.common.TemplateConstants.SHELL_SCRIPT;
import static software.wings.common.TemplateConstants.SSH;

import io.harness.beans.KeyValuePair;
import io.harness.shell.ScriptType;

import software.wings.beans.Variable;
import software.wings.beans.VariableType;
import software.wings.beans.command.ExecCommandUnit;
import software.wings.beans.command.ProcessCheckRunningCommandUnit;
import software.wings.beans.template.BaseTemplate;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.artifactsource.ArtifactSourceTemplate;
import software.wings.beans.template.artifactsource.CustomArtifactSourceTemplate;
import software.wings.beans.template.artifactsource.CustomRepositoryMapping;
import software.wings.beans.template.command.HttpTemplate;
import software.wings.beans.template.command.PcfCommandTemplate;
import software.wings.beans.template.command.ShellScriptTemplate;
import software.wings.beans.template.command.SshCommandTemplate;

import java.util.Arrays;
import java.util.Collections;

public class TemplateLibaryYamlConstants {
  //   Common Constants.
  public static final TemplateFolder rootTemplateFolder =
      TemplateFolder.builder().name("Harness").appId(GLOBAL_APP_ID).accountId(GLOBAL_ACCOUNT_ID).build();

  public static final Variable variable = aVariable()
                                              .description("asd")
                                              .name("abc")
                                              .value("xyz")
                                              .fixed(false)
                                              .mandatory(false)
                                              .type(VariableType.TEXT)
                                              .build();

  //  Shell script constants.
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

  public static final Template templateForSetup = Template.builder()
                                                      .type(SHELL_SCRIPT)
                                                      .name("test-shell-script")
                                                      .accountId(GLOBAL_ACCOUNT_ID)
                                                      .appId(GLOBAL_APP_ID)
                                                      .build();

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

  public static final Template expectedTemplate = Template.builder()
                                                      .name("test-shell-script")
                                                      .accountId(GLOBAL_ACCOUNT_ID)
                                                      .type(SHELL_SCRIPT)
                                                      .variables(Arrays.asList(variable))
                                                      .appId(GLOBAL_APP_ID)
                                                      .templateObject(baseTemplateObject)
                                                      .build();

  // http template constants.
  public static final String httpTemplateName = "test-http-template";

  public static final String VALID_HTTP_TEMPLATE_WITH_VARIABLE = "harnessApiVersion: '1.0'\n"
      + "type: HTTP\n"
      + "assertion: '200'\n"
      + "body: xyz\n"
      + "headers:\n"
      + "- key: content-type\n"
      + "  value: application/json\n"
      + "method: GET\n"
      + "timeoutMillis: 1000000\n"
      + "url: harness.io\n"
      + "variables:\n"
      + "- description: asd\n"
      + "  name: abc\n"
      + "  value: xyz\n";
  public static final String INVALID_HTTP_TEMPLATE_WITH_VARIABLE = "harnessApiVersion: '1.0'\n"
      + "type: HTTP\n"
      + "assertion: '200'\n"
      + "body: xyz\n"
      + "header: aslkdn\n"
      + "method: RANDOM\n" + // Error in this line.
      "timeoutMillis: 1000000\n"
      + "url: harness.io\n"
      + "variables:\n"
      + "- description: aslkdn\n"
      + "  name: abc\n"
      + "  value: xyz\n";
  public static final BaseTemplate baseHttpTemplateObject =
      HttpTemplate.builder()
          .url("harness.io")
          .assertion("200")
          .body("xyz")
          .headers(
              Collections.singletonList(KeyValuePair.builder().key("content-type").value("application/json").build()))
          .method("GET")
          .timeoutMillis(1000000)
          .build();
  public static final String HTTP_TEMPLATE_VALID_YAML_FILE_PATH =
      "Setup/Template Library/Harness/" + httpTemplateName + ".yaml";
  public static final String INVALID_HTTP_TEMPLATE_VALID_YAML_FILE_PATH =
      "Setup/Template Library/" + httpTemplateName + ".yaml";
  public static final Template httpTemplateForSetup = Template.builder()
                                                          .type(HTTP)
                                                          .name(httpTemplateName)
                                                          .templateObject(baseHttpTemplateObject)
                                                          .accountId(GLOBAL_ACCOUNT_ID)
                                                          .appId(GLOBAL_APP_ID)
                                                          .build();

  public static final Template expectedHttpTemplate = Template.builder()
                                                          .name(httpTemplateName)
                                                          .accountId(GLOBAL_ACCOUNT_ID)
                                                          .type(HTTP)
                                                          .variables(Arrays.asList(variable))
                                                          .appId(GLOBAL_APP_ID)
                                                          .templateObject(baseHttpTemplateObject)
                                                          .build();

  //  Command Template Constants.
  public static final String VALID_COMMAND_TEMPLATE_WITHOUT_VARIABLE = "harnessApiVersion: '1.0'\n"
      + "type: SSH\n"
      + "commandUnitType: START\n"
      + "commandUnits:\n"
      + "- command: ./startup.sh\n"
      + "  commandUnitType: EXEC\n"
      + "  deploymentType: SSH\n"
      + "  name: Start Service\n"
      + "  scriptType: BASH\n"
      + "  workingDirectory: ${RuntimePath}/tomcat/bin\n"
      + "- command: echo 0\n"
      + "  commandUnitType: PROCESS_CHECK_RUNNING\n"
      + "  deploymentType: SSH\n"
      + "  name: Process Running\n"
      + "  scriptType: BASH\n";

  public static final String INVALID_COMMAND_TEMPLATE_WITHOUT_VARIABLE = "harnessApiVersion: '1.0'\n"
      + "type: SSH\n"
      + "commandUnitType: START\n"
      + "commandUnits:\n"
      + "- command: ./startup.sh\n"
      + "  commandUnitType: EXEC\n"
      + "  deploymentType: SSH\n"
      + "  name: Start Service\n"
      + "  scriptType: BASHed\n" + // Error in this line.
      "  workingDirectory: ${RuntimePath}/tomcat/bin\n"
      + "- command: echo 0\n"
      + "  commandUnitType: PROCESS_CHECK_RUNNING\n"
      + "  deploymentType: SSH\n"
      + "  name: Process Running\n"
      + "  scriptType: BASH\n";

  public static final BaseTemplate baseCommandTemplateObject =
      SshCommandTemplate.builder()
          .commandUnits(Arrays.asList(ExecCommandUnit.Builder.anExecCommandUnit()
                                          .withCommandPath("${RuntimePath}/tomcat/bin")
                                          .withCommandString("./startup.sh")
                                          .withName("Start Service")
                                          .withScriptType(ScriptType.BASH)
                                          .build(),
              ProcessCheckRunningCommandUnit.Builder.anExecCommandUnit()
                  .withScriptType(ScriptType.BASH)
                  .withCommandString("echo 0")
                  .withName("Process Running")
                  .build()))
          .commands(Arrays.asList(ExecCommandUnit.Yaml.builder()
                                      .workingDirectory("${RuntimePath}/tomcat/bin")
                                      .command("./startup.sh")
                                      .scriptType("BASH")
                                      .name("Start Service")
                                      .deploymentType("SSH")
                                      .build(),
              ProcessCheckRunningCommandUnit.Yaml.builder()
                  .command("echo 0")
                  .name("Process Running")
                  .deploymentType("SSH")
                  .scriptType("BASH")
                  .build()))
          .build();

  public static String commandTemplateName = "test-command-template";

  public static final String COMMAND_TEMPLATE_VALID_YAML_FILE_PATH =
      "Setup/Template Library/Harness/" + commandTemplateName + ".yaml";

  public static final String INVALID_COMMAND_TEMPLATE_VALID_YAML_FILE_PATH =
      "Setup/Template Library/" + commandTemplateName + ".yaml";

  public static final Template commandTemplateForSetup =
      Template.builder().type(SSH).name(commandTemplateName).accountId(GLOBAL_ACCOUNT_ID).appId(GLOBAL_APP_ID).build();

  public static final Template expectedCommandTemplate = Template.builder()
                                                             .name(commandTemplateName)
                                                             .type(SSH)
                                                             .accountId(GLOBAL_ACCOUNT_ID)
                                                             .appId(GLOBAL_APP_ID)
                                                             .version(1L)
                                                             .templateObject(baseCommandTemplateObject)
                                                             .build();

  public static final Template expectedReturnCommandTemplate = Template.builder()
                                                                   .name(commandTemplateName)
                                                                   .type(SSH)
                                                                   .accountId(GLOBAL_ACCOUNT_ID)
                                                                   .appId(GLOBAL_APP_ID)
                                                                   .templateObject(baseCommandTemplateObject)
                                                                   .version(1)
                                                                   .build();

  //  Ref command.
  public static String refCommandTemplateName = "test-command-template-ref";
  public static final String commandTemplateUri = "Harness/" + commandTemplateName + ":1";

  public static final String REF_COMMAND_TEMPLATE_VALID_YAML_FILE_PATH =
      "Setup/Template Library/Harness/" + refCommandTemplateName + ".yaml";

  public static final String REF_COMMAND_UNIT = "commandUnitType: COMMAND\n"
      + "name: child\n"
      + "templateUri: " + commandTemplateUri;

  public static final String EXEC_COMMAND_UNIT = "command: ./startup.sh\n"
      + "commandUnitType: EXEC\n"
      + "deploymentType: SSH\n"
      + "name: Start Service\n"
      + "scriptType: BASH\n"
      + "workingDirectory: ${RuntimePath}/tomcat/bin\n";

  public static final String commandRefUuid = "refuuid";
  public static final String commandTemplateRefUri = "Harness/abc:1";

  // artifact source template constants.
  public static final String artifactTemplateName = "test-artifact-template";

  public static final String VALID_ARTIFACT_TEMPLATE_WITH_VARIABLE = "harnessApiVersion: '1.0'\n"
      + "type: ARTIFACT_SOURCE\n"
      + "customRepositoryMapping:\n"
      + "  artifactAttributes:\n"
      + "  - mappedAttribute: abc\n"
      + "    relativePath: xyz\n"
      + "  artifactRoot: abc1\n"
      + "  buildNoPath: xyz1\n"
      + "script: echo 1\n"
      + "sourceType: CUSTOM\n"
      + "timeout: '60'\n"
      + "variables:\n"
      + "- description: asd\n"
      + "  name: abc\n"
      + "  value: xyz\n";

  public static final String INVALID_ARTIFACT_TEMPLATE_WITH_VARIABLE = "harnessApiVersion: '1.0'\n"
      + "type: ARTIFACT_SOURCE\n"
      + "customRepositoryMapping:\n"
      + "  artifactAttributes:\n"
      + "  - mappedAttribute: abc\n"
      + "    relativePath: xyz\n"
      + "  artifactRoot: abc1\n"
      + "  buildNoPath: xyz1\n"
      + "script: echo 1\n"
      + "sourceType: CUSTOM\n"
      + "timeout: 60\n" + // Error in this line.
      "variables:\n"
      + "- description: asd\n"
      + "  name: abc\n"
      + "  value: xyz\n";

  public static final BaseTemplate baseArtifactTemplateObject =
      ArtifactSourceTemplate.builder()
          .artifactSource(CustomArtifactSourceTemplate.builder()
                              .script("echo 1")
                              .timeoutSeconds("60")
                              .customRepositoryMapping(CustomRepositoryMapping.builder()
                                                           .artifactAttributes(Arrays.asList(
                                                               CustomRepositoryMapping.AttributeMapping.builder()
                                                                   .relativePath("xyz")
                                                                   .mappedAttribute("abc")
                                                                   .build()))
                                                           .artifactRoot("abc1")
                                                           .buildNoPath("xyz1")
                                                           .build())
                              .build())
          .build();
  public static final String ARTIFACT_TEMPLATE_VALID_YAML_FILE_PATH =
      "Setup/Template Library/Harness/" + artifactTemplateName + ".yaml";
  public static final String ARTIFACT_TEMPLATE_INVALID_YAML_FILE_PATH =
      "Setup/Template Library/" + artifactTemplateName + ".yaml";
  public static final Template artifactTemplateForSetup = Template.builder()
                                                              .type(ARTIFACT_SOURCE)
                                                              .name(artifactTemplateName)
                                                              .accountId(GLOBAL_ACCOUNT_ID)
                                                              .appId(GLOBAL_APP_ID)
                                                              .build();

  public static final Template expectedArtifactTemplate = Template.builder()
                                                              .name(artifactTemplateName)
                                                              .accountId(GLOBAL_ACCOUNT_ID)
                                                              .type(ARTIFACT_SOURCE)
                                                              .variables(Arrays.asList(variable))
                                                              .appId(GLOBAL_APP_ID)
                                                              .templateObject(baseArtifactTemplateObject)
                                                              .build();

  //  PcfCommandTemplate constants.
  public static final String PCF_COMMAND_TEMPLATE_VALID_YAML_FILE_PATH =
      "Setup/Template Library/Harness/test-pcf-command.yaml";

  public static final Template PCfCommandTemplateForSetup = Template.builder()
                                                                .type(PCF_PLUGIN)
                                                                .name("test-pcf-command")
                                                                .accountId(GLOBAL_ACCOUNT_ID)
                                                                .appId(GLOBAL_APP_ID)
                                                                .build();

  public static final BaseTemplate basePcfCommandTemplateObject =
      PcfCommandTemplate.builder().scriptString("echo asd").timeoutIntervalInMinutes(5).build();

  public static final Template expectedPcfCommandTemplate = Template.builder()
                                                                .name("test-pcf-command")
                                                                .accountId(GLOBAL_ACCOUNT_ID)
                                                                .type(PCF_PLUGIN)
                                                                .variables(Arrays.asList(variable))
                                                                .appId(GLOBAL_APP_ID)
                                                                .templateObject(basePcfCommandTemplateObject)
                                                                .build();

  public static final String VALID_PCF_COMMAND_TEMPLATE_WITH_VARIABLE = "harnessApiVersion: '1.0'\n"
      + "type: PCF_PLUGIN\n"
      + "scriptString: echo asd\n"
      + "timeoutIntervalInMinutes: 5\n"
      + "variables:\n"
      + "- description: asd\n"
      + "  name: abc\n"
      + "  value: xyz\n";

  public static final Template expectedPcfCommandTemplateWithoutVariable =
      Template.builder()
          .name("test-pcf-command")
          .accountId(GLOBAL_ACCOUNT_ID)
          .appId(GLOBAL_APP_ID)
          .templateObject(basePcfCommandTemplateObject)
          .build();

  public static final String INVALID_PCF_COMMAND_TEMPLATE_WITH_VARIABLE = "harnessApiVersion: '1.0'\n"
      + "type: PCF_PLUGIN\n"
      + "scriptString: echo asd\n"
      + "timeoutIntervalInMinutes: -1\n"
      + "variables:\n"
      + "- description: asd\n"
      + "  name: abc\n"
      + "  value: xyz\n";

  public static final String INVALID_PCF_COMMAND_TEMPLATE_VALID_YAML_FILE_PATH =
      "Setup/Template Library/test-pcf-command.yaml";
}
