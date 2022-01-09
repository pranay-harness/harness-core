/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.security.customsecretsmanager;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.UnexpectedException;

import software.wings.beans.Variable;
import software.wings.beans.template.BaseTemplate;
import software.wings.beans.template.Template;
import software.wings.beans.template.command.ShellScriptTemplate;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerShellScript;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerShellScript.ScriptType;
import software.wings.service.intfc.template.TemplateService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(PL)
class CustomSecretsManagerShellScriptHelper {
  private final TemplateService templateService;

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
