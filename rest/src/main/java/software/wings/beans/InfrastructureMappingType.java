package software.wings.beans;

import static org.joor.Reflect.on;

import com.google.common.base.Charsets;
import com.google.common.base.MoreObjects;
import com.google.common.io.Resources;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import io.harness.exception.WingsException;
import software.wings.stencils.OverridingStencil;
import software.wings.stencils.StencilCategory;
import software.wings.utils.JsonUtils;

import java.net.URL;
import java.util.HashMap;

/**
 * The enum Infra mapping type.
 */
public enum InfrastructureMappingType implements InfrastructureMappingDescriptor {
  /**
   * Physical data center ssh infra mapping type.
   */
  PHYSICAL_DATA_CENTER_SSH(PhysicalInfrastructureMapping.class, "PHYSICAL_DATA_CENTER_SSH", StencilCategory.OTHERS,
      1), /**
           * Aws ssh infra mapping type.
           */
  AWS_SSH(AwsInfrastructureMapping.class, "AWS_SSH", StencilCategory.OTHERS, 2),

  /**
   * Aws ami infrastructure mapping type.
   */
  AWS_AMI(
      AwsAmiInfrastructureMapping.class, "AWS_AMI", StencilCategory.OTHERS, 2), /**
                                                                                 * Aws CodeDeploy infra mapping type.
                                                                                 */
  AWS_AWS_CODEDEPLOY(CodeDeployInfrastructureMapping.class, "AWS_AWS_CODEDEPLOY", StencilCategory.OTHERS,
      3), /**
           * Aws ecs infra mapping type.
           */
  AWS_ECS(EcsInfrastructureMapping.class, "AWS_ECS", StencilCategory.OTHERS,
      4), /**
           * Direct connection to kubernetes infra mapping type.
           */
  DIRECT_KUBERNETES(DirectKubernetesInfrastructureMapping.class, "DIRECT_KUBERNETES", StencilCategory.OTHERS,
      5), /**
           * Gcp kubernetes infra mapping type.
           */
  GCP_KUBERNETES(GcpKubernetesInfrastructureMapping.class, "GCP_KUBERNETES", StencilCategory.OTHERS,
      6), /**
           * Aws aws lambda infrastructure mapping type.
           */
  AWS_AWS_LAMBDA(AwsLambdaInfraStructureMapping.class, "AWS_AWS_LAMBDA", StencilCategory.OTHERS, 7),
  /**
   * Azure kubernetes [AKS] infra mapping type.
   */
  AZURE_KUBERNETES(AzureKubernetesInfrastructureMapping.class, "AZURE_KUBERNETES", StencilCategory.OTHERS, 8),

  PHYSICAL_DATA_CENTER_WINRM(
      PhysicalInfrastructureMappingWinRm.class, "PHYSICAL_DATA_CENTER_WINRM", StencilCategory.OTHERS, 9),

  PCF_PCF(PcfInfrastructureMapping.class, "PCF_PCF", StencilCategory.OTHERS, 10);

  private static final String stencilsPath = "/templates/inframapping/";
  private static final String uiSchemaSuffix = "-InfraMappingUISchema.json";

  private Object uiSchema;
  private JsonNode jsonSchema;

  @JsonIgnore private Class<? extends InfrastructureMapping> infrastructureMappingClass;
  @JsonIgnore private String name;

  private StencilCategory stencilCategory;
  private Integer displayOrder;

  InfrastructureMappingType(Class<? extends InfrastructureMapping> infrastructureMappingClass, String name,
      StencilCategory stencilCategory, Integer displayOrder) {
    this.infrastructureMappingClass = infrastructureMappingClass;
    this.name = name;
    this.stencilCategory = stencilCategory;
    this.displayOrder = displayOrder;
    try {
      uiSchema = readResource(stencilsPath + name() + uiSchemaSuffix);
    } catch (Exception e) {
      uiSchema = new HashMap<String, String>();
    }
    jsonSchema = JsonUtils.jsonSchema(infrastructureMappingClass);
  }

  @Override
  public String getType() {
    return getName();
  }

  @Override
  public Class<? extends InfrastructureMapping> getTypeClass() {
    return infrastructureMappingClass;
  }

  @Override
  public StencilCategory getStencilCategory() {
    return stencilCategory;
  }

  @Override
  public JsonNode getJsonSchema() {
    return jsonSchema;
  }

  @Override
  public Object getUiSchema() {
    return uiSchema;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public OverridingStencil getOverridingStencil() {
    return new OverridingInfrastructureMappingType(this);
  }

  @Override
  public Integer getDisplayOrder() {
    return displayOrder;
  }

  @Override
  public InfrastructureMapping newInstance(String id) {
    return on(infrastructureMappingClass).create().get();
  }

  @Override
  public boolean matches(Object context) {
    return true;
  }

  private Object readResource(String file) {
    try {
      URL url = this.getClass().getResource(file);
      String json = Resources.toString(url, Charsets.UTF_8);
      return JsonUtils.asObject(json, HashMap.class);
    } catch (Exception exception) {
      throw new WingsException("Error in initializing InfrastructureMapping-" + file, exception);
    }
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("uiSchema", uiSchema)
        .add("jsonSchema", jsonSchema)
        .add("infrastructureMappingClass", infrastructureMappingClass)
        .add("name", name)
        .add("stencilCategory", stencilCategory)
        .add("displayOrder", displayOrder)
        .add("type", getType())
        .add("typeClass", getTypeClass())
        .add("overridingStencil", getOverridingStencil())
        .toString();
  }
}
