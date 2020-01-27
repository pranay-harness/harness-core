package software.wings.service.impl.template;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Arrays.asList;

import com.google.inject.Singleton;

import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateType;
import software.wings.beans.template.command.ShellScriptTemplate;

import java.util.List;
import java.util.Map;

@Singleton
public class ShellScriptTemplateProcessor extends StateTemplateProcessor {
  private static final String SCRIPT_TYPE = "scriptType";
  private static final String SCRIPT_STRING = "scriptString";
  private static final String OUTPUT_VARS = "outputVars";
  private static final String TIMEOUT_MILLIS = "timeoutMillis";
  private static final String VARIABLES = "variables";

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
      properties.put(SCRIPT_TYPE, shellScriptTemplate.getScriptType());
    }
    if (isNotEmpty(shellScriptTemplate.getScriptString())) {
      properties.put(SCRIPT_STRING, shellScriptTemplate.getScriptString());
    }
    if (isNotEmpty(shellScriptTemplate.getOutputVars())) {
      properties.put(OUTPUT_VARS, shellScriptTemplate.getOutputVars());
    }

    properties.put(TIMEOUT_MILLIS, shellScriptTemplate.getTimeoutMillis());
  }

  @Override
  public List<String> fetchTemplateProperties() {
    List<String> templateProperties = super.fetchTemplateProperties();
    templateProperties.addAll(asList(SCRIPT_TYPE, SCRIPT_STRING, OUTPUT_VARS, VARIABLES));
    return templateProperties;
  }
}
