package io.harness.template.merger.helpers;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.pms.merger.helpers.MergeHelper.mergeInputSetFormatYamlToOriginYaml;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.exception.NGTemplateException;
import io.harness.pms.merger.helpers.RuntimeInputFormHelper;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;
import io.harness.template.beans.yaml.NGTemplateConfig;
import io.harness.template.entity.TemplateEntity;
import io.harness.template.services.NGTemplateService;
import io.harness.utils.IdentifierRefHelper;
import io.harness.utils.YamlPipelineUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CDC)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class TemplateMergeHelper {
  private static final String TEMPLATE = "template";
  private static final String TEMPLATE_REF = "templateRef";
  private static final String TEMPLATE_VERSION_LABEL = "versionLabel";
  private static final String TEMPLATE_INPUTS = "templateInputs";
  private static final String DUMMY_NODE = "dummy";

  private NGTemplateService templateService;

  /**
   * This method gets the template inputs from template.spec in template yaml.
   * For eg: Template Yaml:
   * template:
   *   identifier: httpTemplate
   *   versionLabel: 1
   *   name: template1
   *   type: Step
   *   spec:
   *     type: Http
   *     spec:
   *       url: <+input>
   *       method: GET
   *     timeout: <+input>
   *
   * Output template inputs yaml:
   * type: Http
   * spec:
   *   url: <+input>
   * timeout: <+input>
   *
   * @param yaml - template yaml
   * @return template inputs yaml
   */
  public String createTemplateInputs(String yaml) {
    try {
      if (isEmpty(yaml)) {
        throw new NGTemplateException("Template yaml to create template inputs cannot be empty");
      }
      YamlField templateYamlField = YamlUtils.readTree(yaml).getNode().getField("template");
      if (templateYamlField == null) {
        log.error("Yaml provided is not a template yaml. Yaml:\n" + yaml);
        throw new NGTemplateException("Yaml provided is not a template yaml.");
      }
      ObjectNode templateNode = (ObjectNode) templateYamlField.getNode().getCurrJsonNode();
      String templateSpec = templateNode.retain("spec").toString();
      if (isEmpty(templateSpec)) {
        log.error("Template yaml provided does not have spec in it.");
        throw new NGTemplateException("Template yaml provided does not have spec in it.");
      }
      String templateInputsYamlWithSpec = RuntimeInputFormHelper.createTemplateFromYaml(templateSpec);
      if (isEmpty(templateInputsYamlWithSpec)) {
        return templateInputsYamlWithSpec;
      }
      JsonNode templateInputsYaml =
          YamlUtils.readTree(templateInputsYamlWithSpec).getNode().getCurrJsonNode().get("spec");
      return YamlUtils.write(templateInputsYaml).replace("---\n", "");
    } catch (IOException e) {
      log.error("Error occurred while creating template inputs " + e);
      throw new NGTemplateException("Error occurred while creating template inputs ", e);
    }
  }

  /**
   * The method replaces all template occurrences in pipeline yaml with information present under template.spec in
   * template yaml. Before we replace template occurrence in pipeline yaml, we merge template inputs provided in
   * pipeline yaml to template.spec in template yaml
   * @param accountId - accountId of pipeline
   * @param orgId - organisationId of pipeline
   * @param projectId - projectId of pipeline
   * @param pipelineYaml - pipeline yaml
   * @return final pipeline yaml with all template occurrences replaced with actual template information.
   */
  public String mergeTemplateSpecToPipelineYaml(String accountId, String orgId, String projectId, String pipelineYaml) {
    JsonNode pipelineJsonNode;
    try {
      pipelineJsonNode = YamlUtils.readTree(pipelineYaml).getNode().getCurrJsonNode();
    } catch (IOException e) {
      log.error("Could not convert yaml to JsonNode. Yaml:\n" + pipelineYaml, e);
      throw new NGTemplateException("Could not convert yaml to JsonNode: " + e.getMessage());
    }

    Map<String, Object> resMap = generateYamlMap(accountId, orgId, projectId, pipelineJsonNode);
    return YamlUtils.write(resMap).replace("---\n", "");
  }

  /**
   * This method iterates recursively on pipeline yaml. Whenever we find a key with "template" we call
   * replaceTemplateOccurrenceWithTemplateSpecYaml() to get the actual template.spec in template yaml.
   */
  private Map<String, Object> generateYamlMap(String accountId, String orgId, String projectId, JsonNode yaml) {
    Map<String, Object> result = new LinkedHashMap<>();
    Set<String> fieldNames = new LinkedHashSet<>();
    yaml.fieldNames().forEachRemaining(fieldNames::add);

    for (String key : fieldNames) {
      JsonNode value = yaml.get(key);
      if (TEMPLATE.equals(key)) {
        value = replaceTemplateOccurrenceWithTemplateSpecYaml(accountId, orgId, projectId, value);
      }
      if (value.getNodeType() == JsonNodeType.ARRAY) {
        Object listProcessResult = generateYamlMapFromList(accountId, orgId, projectId, (ArrayNode) value);
        result.put(key, listProcessResult);
      } else if (value.getNodeType() == JsonNodeType.OBJECT) {
        Map<String, Object> temp = generateYamlMap(accountId, orgId, projectId, value);
        // If it was template key in yaml, we have replace it with the fields in template.spec in template yaml.
        // Hence, we directly put all the keys returned in map, after iterating over them.
        if (TEMPLATE.equals(key)) {
          result.putAll(temp);
        } else {
          result.put(key, temp);
        }
      } else {
        result.put(key, value);
      }
    }

    return result;
  }

  /**
   * This method gets the TemplateEntity from database. Further it gets template yaml and merge template inputs present
   * in pipeline to template.spec in template yaml
   * @param template - template json node present in pipeline yaml
   * @return jsonNode of merged yaml
   */
  private JsonNode replaceTemplateOccurrenceWithTemplateSpecYaml(
      String accountId, String orgId, String projectId, JsonNode template) {
    String identifier = template.get(TEMPLATE_REF).asText();
    String versionLabel = template.get(TEMPLATE_VERSION_LABEL).asText();
    JsonNode templateInputs = template.get(TEMPLATE_INPUTS);

    IdentifierRef templateIdentifierRef = IdentifierRefHelper.getIdentifierRef(identifier, accountId, orgId, projectId);
    Optional<TemplateEntity> templateEntity =
        templateService.get(templateIdentifierRef.getAccountIdentifier(), templateIdentifierRef.getOrgIdentifier(),
            templateIdentifierRef.getProjectIdentifier(), templateIdentifierRef.getIdentifier(), versionLabel, false);
    if (!templateEntity.isPresent()) {
      throw new NGTemplateException(String.format(
          "The template identifier %s and version label %s does not exist. Could not replace this template",
          templateIdentifierRef.getIdentifier(), versionLabel));
    }
    String templateYaml = templateEntity.get().getYaml();

    JsonNode templateSpec;
    try {
      NGTemplateConfig templateConfig = YamlPipelineUtils.read(templateYaml, NGTemplateConfig.class);
      templateSpec = templateConfig.getTemplateInfoConfig().getSpec();
    } catch (IOException e) {
      log.error("Could not read template yaml", e);
      throw new NGTemplateException("Could not read template yaml: " + e.getMessage());
    }

    if (templateInputs == null) {
      return templateSpec;
    }

    // TODO[Inder]: Add validation to check if template inputs provided in pipeline yaml are valid.
    return mergeTemplateInputsToTemplateSpecInTemplateYaml(templateInputs, templateSpec);
  }

  /**
   * This method merges template inputs provided in pipeline yaml to template spec in template yaml.
   * @param templateInputs - template runtime info provided in pipeline yaml
   * @param templateSpec - template spec present in template yaml
   * @return jsonNode of merged yaml
   */
  private JsonNode mergeTemplateInputsToTemplateSpecInTemplateYaml(JsonNode templateInputs, JsonNode templateSpec) {
    Map<String, JsonNode> dummyTemplateSpecMap = new LinkedHashMap<>();
    dummyTemplateSpecMap.put(DUMMY_NODE, templateSpec);

    Map<String, JsonNode> dummyTemplateInputsMap = new LinkedHashMap<>();
    dummyTemplateInputsMap.put(DUMMY_NODE, templateInputs);

    String mergedYaml = mergeInputSetFormatYamlToOriginYaml(YamlUtils.write(dummyTemplateSpecMap).replace("---\n", ""),
        YamlUtils.write(dummyTemplateInputsMap).replace("---\n", ""));

    try {
      return YamlUtils.readTree(mergedYaml).getNode().getCurrJsonNode().get(DUMMY_NODE);
    } catch (IOException e) {
      log.error("Could not convert merged yaml to JsonNode. Yaml:\n" + mergedYaml, e);
      throw new NGTemplateException("Could not convert merged yaml to JsonNode: " + e.getMessage());
    }
  }

  /**
   * This method iterates over lists in yaml.
   */
  private Object generateYamlMapFromList(String accountId, String orgId, String projectId, ArrayNode list) {
    if (list == null || list.get(0) == null) {
      return list;
    }

    JsonNode firstNode = list.get(0);
    if (firstNode.getNodeType() != JsonNodeType.OBJECT) {
      return list;
    }

    List<Object> finalList = new ArrayList<>();
    list.forEach(element -> finalList.add(generateYamlMap(accountId, orgId, projectId, element)));
    return finalList;
  }
}
