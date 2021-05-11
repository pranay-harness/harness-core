package io.harness.cvng.core.services.impl;

import static io.harness.yaml.schema.beans.SchemaConstants.DEFINITIONS_NODE;

import io.harness.EntityType;
import io.harness.cvng.cdng.beans.CVNGStepInfo;
import io.harness.cvng.core.services.api.CVNGYamlSchemaService;
import io.harness.encryption.Scope;
import io.harness.yaml.schema.SchemaGeneratorUtils;
import io.harness.yaml.schema.YamlSchemaGenerator;
import io.harness.yaml.schema.YamlSchemaProvider;
import io.harness.yaml.schema.beans.PartialSchemaDTO;
import io.harness.yaml.utils.YamlSchemaUtils;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import lombok.AllArgsConstructor;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class CVNGYamlSchemaServiceImpl implements CVNGYamlSchemaService {
  private static final String CVNG_NAMESPACE = "cvng";
  private final YamlSchemaProvider yamlSchemaProvider;
  private final YamlSchemaGenerator yamlSchemaGenerator;

  @Override
  public PartialSchemaDTO getDeploymentStageYamlSchema(String orgIdentifier, String projectIdentifier, Scope scope) {
    JsonNode deploymentSteps =
        yamlSchemaProvider.getYamlSchema(EntityType.DEPLOYMENT_STEPS, orgIdentifier, projectIdentifier, scope);
    JsonNode definitions = deploymentSteps.get(DEFINITIONS_NODE);
    yamlSchemaGenerator.modifyRefsNamespace(deploymentSteps, CVNG_NAMESPACE);
    ObjectMapper mapper = SchemaGeneratorUtils.getObjectMapperForSchemaGeneration();
    JsonNode node = mapper.createObjectNode().set(CVNG_NAMESPACE, definitions);
    JsonNode partialCVNGSchema = ((ObjectNode) deploymentSteps).set(DEFINITIONS_NODE, node);
    return PartialSchemaDTO.builder()
        .namespace(CVNG_NAMESPACE)
        .nodeName(YamlSchemaUtils.getSwaggerName(CVNGStepInfo.class))
        .schema(partialCVNGSchema)
        .nodeType(getVerifyStepTypeName())
        .build();
  }

  private String getVerifyStepTypeName() {
    JsonTypeName annotation = CVNGStepInfo.class.getAnnotation(JsonTypeName.class);
    return annotation.value();
  }
}
