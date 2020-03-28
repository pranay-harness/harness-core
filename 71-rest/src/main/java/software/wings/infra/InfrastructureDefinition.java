package software.wings.infra;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.data.validator.EntityName;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.NameAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.api.CloudProviderType;
import software.wings.api.DeploymentType;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.entityinterface.ApplicationAccess;
import software.wings.service.impl.yaml.handler.InfraDefinition.CloudProviderInfrastructureYaml;
import software.wings.yaml.BaseEntityYaml;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;

@Indexes({
  @Index(options = @IndexOptions(name = "infraDefinitionIdx", unique = true),
      fields = { @Field("appId")
                 , @Field("envId"), @Field("name") })
  ,
      @Index(options = @IndexOptions(name = "infrastructure_cloudProviderId"),
          fields = { @Field("infrastructure.cloudProviderId") })
})

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "InfrastructureDefinitionKeys")
@Entity(value = "infrastructureDefinitions", noClassnameStored = true)
@HarnessEntity(exportable = true)
public class InfrastructureDefinition implements PersistentEntity, UuidAware, NameAccess, CreatedAtAware,
                                                 CreatedByAware, UpdatedAtAware, UpdatedByAware, ApplicationAccess {
  @Id private String uuid;
  @SchemaIgnore private EmbeddedUser createdBy;
  @SchemaIgnore private long createdAt;
  @NotEmpty @EntityName private String name;
  @SchemaIgnore private EmbeddedUser lastUpdatedBy;
  @SchemaIgnore @NotNull private long lastUpdatedAt;
  @NotNull protected String appId;
  private String provisionerId;
  @NotNull private CloudProviderType cloudProviderType;
  @NotNull private DeploymentType deploymentType;
  @NotNull private InfraMappingInfrastructureProvider infrastructure;
  private List<String> scopedToServices;
  @NotNull private String envId;
  private boolean sample;

  @JsonIgnore
  public InfrastructureMapping getInfraMapping() {
    InfrastructureMapping infrastructureMapping = infrastructure.getInfraMapping();
    infrastructureMapping.setAppId(appId);
    infrastructureMapping.setEnvId(envId);
    infrastructureMapping.setDeploymentType(deploymentType.name());
    infrastructureMapping.setComputeProviderType(cloudProviderType.name());
    infrastructureMapping.setProvisionerId(provisionerId);
    return infrastructureMapping;
  }

  public InfrastructureDefinition cloneForUpdate() {
    return InfrastructureDefinition.builder()
        .name(getName())
        .provisionerId(getProvisionerId())
        .cloudProviderType(getCloudProviderType())
        .deploymentType(getDeploymentType())
        .infrastructure(getInfrastructure())
        .scopedToServices(getScopedToServices())
        .build();
  }

  /**
   * The type Yaml.
   */
  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends BaseEntityYaml {
    private String name;
    private CloudProviderType cloudProviderType;
    private DeploymentType deploymentType;
    @NotNull private List<CloudProviderInfrastructureYaml> infrastructure = new ArrayList<>();
    private List<String> scopedServices;
    private String provisioner;

    @Builder
    public Yaml(String type, String harnessApiVersion, CloudProviderType cloudProviderType,
        DeploymentType deploymentType, List<CloudProviderInfrastructureYaml> infrastructure,
        List<String> scopedServices, String provisioner) {
      super(type, harnessApiVersion);
      setCloudProviderType(cloudProviderType);
      setDeploymentType(deploymentType);
      setInfrastructure(infrastructure);
      setScopedServices(scopedServices);
      setProvisioner(provisioner);
    }
  }
}
