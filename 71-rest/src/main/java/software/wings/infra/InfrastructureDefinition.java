package software.wings.infra;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.data.validator.EntityName;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.NgUniqueIndex;
import io.harness.persistence.AccountAccess;
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
import org.mongodb.morphia.annotations.Id;
import software.wings.api.CloudProviderType;
import software.wings.api.DeploymentType;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.entityinterface.ApplicationAccess;
import software.wings.service.impl.yaml.handler.InfraDefinition.CloudProviderInfrastructureYaml;
import software.wings.service.intfc.customdeployment.CustomDeploymentTypeAware;
import software.wings.yaml.BaseEntityYaml;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;

@NgUniqueIndex(name = "infraDefinitionIdx", fields = { @Field("appId")
                                                       , @Field("envId"), @Field("name") })
@CdIndex(name = "infrastructure_cloudProviderId", fields = { @Field("infrastructure.cloudProviderId") })
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "InfrastructureDefinitionKeys")
@Entity(value = "infrastructureDefinitions", noClassnameStored = true)
@HarnessEntity(exportable = true)
public class InfrastructureDefinition
    implements PersistentEntity, UuidAware, NameAccess, CreatedAtAware, CreatedByAware, UpdatedAtAware, UpdatedByAware,
               ApplicationAccess, CustomDeploymentTypeAware, AccountAccess {
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
  @FdIndex private String accountId;

  /*
  Support for Custom Deployment
   */
  private String deploymentTypeTemplateId;
  private transient String customDeploymentName;

  @JsonIgnore
  public InfrastructureMapping getInfraMapping() {
    InfrastructureMapping infrastructureMapping = infrastructure.getInfraMapping();
    infrastructureMapping.setAccountId(accountId);
    infrastructureMapping.setAppId(appId);
    infrastructureMapping.setEnvId(envId);
    infrastructureMapping.setDeploymentType(deploymentType.name());
    infrastructureMapping.setComputeProviderType(cloudProviderType.name());
    infrastructureMapping.setProvisionerId(provisionerId);
    infrastructureMapping.setCustomDeploymentTemplateId(deploymentTypeTemplateId);
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
        .accountId(getAccountId())
        .deploymentTypeTemplateId(deploymentTypeTemplateId)
        .customDeploymentName(customDeploymentName)
        .build();
  }

  @Override
  public void setDeploymentTypeName(String theCustomDeploymentName) {
    customDeploymentName = theCustomDeploymentName;
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

    /*
     Support for Custom Deployment
      */
    private String deploymentTypeTemplateUri;

    @Builder
    public Yaml(String type, String harnessApiVersion, CloudProviderType cloudProviderType,
        DeploymentType deploymentType, List<CloudProviderInfrastructureYaml> infrastructure,
        List<String> scopedServices, String provisioner, String deploymentTypeTemplateUri) {
      super(type, harnessApiVersion);
      setCloudProviderType(cloudProviderType);
      setDeploymentType(deploymentType);
      setInfrastructure(infrastructure);
      setScopedServices(scopedServices);
      setProvisioner(provisioner);
      setDeploymentTypeTemplateUri(deploymentTypeTemplateUri);
    }
  }
}
