package io.harness.template.handler;

import static io.harness.ng.core.template.TemplateEntityConstants.STEP;
import static io.harness.template.beans.NGTemplateConstants.IDENTIFIER;
import static io.harness.template.beans.NGTemplateConstants.NAME;

import static java.util.Arrays.asList;

import io.harness.ng.core.template.TemplateEntityType;
import io.harness.pms.yaml.YamlField;

import java.util.HashMap;
import java.util.Map;

public class DummyParallelYamlConversionHandler implements YamlConversionHandler {
  @Override
  public String getRootField(TemplateEntityType templateEntityType) {
    return "dummyParallel";
  }

  @Override
  public TemplateYamlConversionData getAdditionalFieldsToAdd(
      TemplateEntityType templateEntityType, YamlField yamlField) {
    Map<String, Object> fieldsToAdd = new HashMap<>();
    fieldsToAdd.put(IDENTIFIER, IDENTIFIER);
    fieldsToAdd.put(NAME, NAME);
    TemplateYamlConversionRecord conversionRecord1 =
        TemplateYamlParallelConversionRecord.builder().fieldsToAdd(fieldsToAdd).path("dummyParallel/spec").build();

    Map<String, Object> fieldsToAdd2 = new HashMap<>();
    fieldsToAdd2.put(STEP, fieldsToAdd);
    TemplateYamlConversionRecord conversionRecord2 = TemplateYamlParallelConversionRecord.builder()
                                                         .fieldsToAdd(fieldsToAdd2)
                                                         .path("dummyParallel/spec/execution/steps/[0]")
                                                         .build();
    return TemplateYamlConversionData.builder()
        .templateYamlConversionRecordList(asList(conversionRecord1, conversionRecord2))
        .build();
  }
}
