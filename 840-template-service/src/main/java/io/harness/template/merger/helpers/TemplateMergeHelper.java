package io.harness.template.merger.helpers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.NGTemplateException;
import io.harness.pms.merger.helpers.YamlTemplateHelper;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CDC)
@Slf4j
public class TemplateMergeHelper {
  public String createTemplateInputs(String yaml) {
    try {
      if (EmptyPredicate.isEmpty(yaml)) {
        throw new NGTemplateException("Template yaml to create template inputs cannot be empty");
      }
      YamlField templateYamlField = YamlUtils.readTree(yaml).getNode().getField("template");
      if (templateYamlField == null) {
        log.error("Yaml provided is not a template yaml. Yaml:\n" + yaml);
        throw new NGTemplateException("Yaml provided is not a template yaml.");
      }
      ObjectNode templateNode = (ObjectNode) templateYamlField.getNode().getCurrJsonNode();
      String templateSpec = templateNode.retain("spec").toString();
      if (EmptyPredicate.isEmpty(templateSpec)) {
        log.error("Template yaml provided does not have spec in it.");
        throw new NGTemplateException("Template yaml provided does not have spec in it.");
      }
      return YamlTemplateHelper.createTemplateFromYaml(templateSpec);

    } catch (IOException e) {
      log.error("Error occurred while creating template inputs " + e);
      throw new NGTemplateException("Error occurred while creating template inputs ", e);
    }
  }
}
