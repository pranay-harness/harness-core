package io.harness.template.handler;

import static io.harness.ng.core.template.TemplateEntityConstants.STEP;
import static io.harness.template.beans.NGTemplateConstants.IDENTIFIER;
import static io.harness.template.beans.NGTemplateConstants.NAME;

import static java.util.Arrays.asList;

import io.harness.ng.core.template.TemplateEntityType;
import io.harness.pms.yaml.YamlField;

import java.util.HashMap;
import java.util.Map;

public class DummyReplaceYamlConversionHandler implements YamlConversionHandler {
  @Override
  public String getRootField(TemplateEntityType templateEntityType) {
    return "dummyReplace";
  }

  @Override
  public TemplateYamlConversionData getAdditionalFieldsToAdd(
      TemplateEntityType templateEntityType, YamlField yamlField) {
    Map<String, Object> fieldsToAdd = new HashMap<>();
    fieldsToAdd.put(IDENTIFIER, IDENTIFIER);
    fieldsToAdd.put(NAME, NAME);
    TemplateYamlConversionRecord conversionRecord1 = TemplateYamlReplaceConversionRecord.builder()
                                                         .fieldsToAdd(getRootField(templateEntityType))
                                                         .path("dummyReplace/spec/execution/steps/[0]/step/name")
                                                         .build();

    Map<String, Object> fieldsToAdd2 = new HashMap<>();
    fieldsToAdd2.put(STEP, fieldsToAdd);
    TemplateYamlConversionRecord conversionRecord2 = TemplateYamlReplaceConversionRecord.builder()
                                                         .fieldsToAdd(fieldsToAdd2)
                                                         .path("dummyReplace/spec/execution/steps/[1]")
                                                         .build();
    return TemplateYamlConversionData.builder()
        .templateYamlConversionRecordList(asList(conversionRecord1, conversionRecord2))
        .build();
  }
}
