package software.wings.service.impl.template;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Arrays.asList;

import com.google.inject.Singleton;

import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateType;
import software.wings.beans.template.command.PcfCommandTemplate;

import java.util.List;
import java.util.Map;

@Singleton
public class PcfCommandTemplateProcessor extends StateTemplateProcessor {
  private static final String SCRIPT_STRING = "scriptString";
  private static final String TIMEOUT_MINS = "timeoutIntervalInMinutes";
  private static final String VARIABLES = "variables";

  @Override
  public void transform(Template template, Map<String, Object> properties) {
    PcfCommandTemplate pcfCommandTemplate = (PcfCommandTemplate) template.getTemplateObject();

    if (isNotEmpty(pcfCommandTemplate.getScriptString())) {
      properties.put(SCRIPT_STRING, pcfCommandTemplate.getScriptString());
    }

    properties.put(TIMEOUT_MINS, pcfCommandTemplate.getTimeoutIntervalInMinutes());
  }

  @Override
  public TemplateType getTemplateType() {
    return TemplateType.PCF_PLUGIN;
  }

  @Override
  public List<String> fetchTemplateProperties() {
    List<String> templateProperties = super.fetchTemplateProperties();
    templateProperties.addAll(asList(SCRIPT_STRING, TIMEOUT_MINS, VARIABLES));
    return templateProperties;
  }
}
