package software.wings.service.impl.security.customsecretsmanager;

import com.google.inject.Inject;

import io.harness.exception.UnexpectedException;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.Variable;
import software.wings.beans.template.BaseTemplate;
import software.wings.beans.template.Template;
import software.wings.beans.template.command.ShellScriptTemplate;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerShellScript;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerShellScript.ScriptType;
import software.wings.service.intfc.template.TemplateService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

class CustomSecretsManagerShellScriptHelper {
  private TemplateService templateService;

  @Inject
  CustomSecretsManagerShellScriptHelper(TemplateService templateService) {
    this.templateService = templateService;
  }

  CustomSecretsManagerShellScript getShellScript(@NotEmpty String accountId, @NotEmpty String templateId) {
    Template template = templateService.get(accountId, templateId, null);
    BaseTemplate baseTemplate = Optional.of(template.getTemplateObject()).<UnexpectedException>orElseThrow(() -> {
      String errorMessage = String.format("Base template for template with id %s does not exist", templateId);
      throw new UnexpectedException(errorMessage);
    });
    if (!(baseTemplate instanceof ShellScriptTemplate)) {
      String errorMessage =
          String.format("Base template for template with id %s is not a Shell Script Template", templateId);
      throw new UnexpectedException(errorMessage);
    }
    List<String> variables = template.getVariables() == null
        ? new ArrayList<>()
        : template.getVariables().stream().map(Variable::getName).collect(Collectors.toList());

    ShellScriptTemplate shellScriptTemplate = (ShellScriptTemplate) baseTemplate;
    return CustomSecretsManagerShellScript.builder()
        .scriptString(shellScriptTemplate.getScriptString())
        .scriptType(ScriptType.valueOf(shellScriptTemplate.getScriptType()))
        .variables(variables)
        .timeoutMillis(shellScriptTemplate.getTimeoutMillis())
        .build();
  }
}
