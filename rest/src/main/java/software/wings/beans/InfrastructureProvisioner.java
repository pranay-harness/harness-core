package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.mongodb.client.model.CollationStrength;
import io.harness.data.validator.Trimmed;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Collation;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.yaml.BaseEntityYaml;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeInfo(use = Id.NAME, property = "infrastructureProvisionerType")
@Entity(value = "infrastructureProvisioner")
@NoArgsConstructor
@Indexes(@Index(options = @IndexOptions(
                    name = "collation", collation = @Collation(locale = "en", strength = CollationStrength.PRIMARY)),
    fields = { @Field("name") }))
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

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public abstract static class Yaml extends BaseEntityYaml {
    private String name;
    private String description;
    private String infrastructureProvisionerType;
    private List<NameValuePair.Yaml> variables;
    private List<InfrastructureMappingBlueprint.Yaml> mappingBlueprints;

    public Yaml(String type, String harnessApiVersion, String name, String description,
        String infrastructureProvisionerType, List<NameValuePair.Yaml> variables,
        List<InfrastructureMappingBlueprint.Yaml> mappingBlueprints) {
      super(type, harnessApiVersion);
      setName(name);
      setDescription(description);
      setInfrastructureProvisionerType(infrastructureProvisionerType);
      setMappingBlueprints(mappingBlueprints);
    }
  }
}
