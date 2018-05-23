package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import io.harness.data.validator.Trimmed;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeInfo(use = Id.NAME, property = "infrastructureProvisionerType")
@Entity(value = "infrastructureProvisioner")
@NoArgsConstructor
public abstract class InfrastructureProvisioner extends Base {
  public static final String INFRASTRUCTURE_PROVISIONER_TYPE_KEY = "infrastructureProvisionerType";
  public static final String MAPPING_BLUEPRINTS_KEY = "mappingBlueprints";
  public static final String NAME_KEY = "name";

  @NotEmpty @Trimmed private String name;
  private String description;
  @NotEmpty private String infrastructureProvisionerType;
  private List<NameValuePair> variables;
  List<InfrastructureMappingBlueprint> mappingBlueprints;

  public InfrastructureProvisioner(String name, String description, String infrastructureProvisionerType,
      List<NameValuePair> variables, List<InfrastructureMappingBlueprint> mappingBlueprints, String uuid, String appId,
      EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy, long lastUpdatedAt, List<String> keywords,
      String entityYamlPath) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, keywords, entityYamlPath);
    this.name = name;
    this.description = description;
    this.infrastructureProvisionerType = infrastructureProvisionerType;
    this.variables = variables;
    this.mappingBlueprints = mappingBlueprints;
  }
  public abstract String variableKey();
}
