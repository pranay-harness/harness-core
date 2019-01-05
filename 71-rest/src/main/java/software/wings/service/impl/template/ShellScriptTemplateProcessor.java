package software.wings.service.impl.template;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Singleton;

import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateType;
import software.wings.beans.template.command.ShellScriptTemplate;

import java.util.Map;

@Singleton
public class ShellScriptTemplateProcessor extends StateTemplateProcessor {
  @Override
  public TemplateType getTemplateType() {
    return TemplateType.SHELL_SCRIPT;
  }

  @Override
  public void loadDefaultTemplates(String accountId, String accountName) {
    //    super.loadDefaultTemplates(Arrays.asList(SHELL_SCRIPT_EXAMPLE), accountId, accountName);
  }

  @Override
  public void transform(Template template, Map<String, Object> properties) {
    ShellScriptTemplate shellScriptTemplate = (ShellScriptTemplate) template.getTemplateObject();
    if (isNotEmpty(shellScriptTemplate.getScriptType())) {
      properties.put("scriptType", shellScriptTemplate.getScriptType());
    }
    if (isNotEmpty(shellScriptTemplate.getScriptString())) {
      properties.put("scriptString", shellScriptTemplate.getScriptString());
    }
    if (isNotEmpty(shellScriptTemplate.getOutputVars())) {
      properties.put("outputVars", shellScriptTemplate.getOutputVars());
    }

    properties.put("timeoutMillis", shellScriptTemplate.getTimeoutMillis());
  }
}
