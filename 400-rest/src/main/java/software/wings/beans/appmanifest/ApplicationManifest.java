package software.wings.beans.appmanifest;

import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.NgUniqueIndex;
import io.harness.persistence.AccountAccess;

import software.wings.beans.Base;
import software.wings.beans.GitFileConfig;
import software.wings.beans.HelmChartConfig;
import software.wings.beans.HelmCommandFlag;
import software.wings.helpers.ext.kustomize.KustomizeConfig;
import software.wings.yaml.BaseEntityYaml;

import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Transient;

@NgUniqueIndex(
    name = "appManifestIdx", fields = { @Field("appId")
                                        , @Field("envId"), @Field("serviceId"), @Field("kind") })
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "ApplicationManifestKeys")
@Entity("applicationManifests")
@HarnessEntity(exportable = true)
public class ApplicationManifest extends Base implements AccountAccess {
  @FdIndex private String accountId;
  private String serviceId;
  private String envId;
  private AppManifestKind kind;
  @NonNull private StoreType storeType;
  private GitFileConfig gitFileConfig;
  private HelmChartConfig helmChartConfig;
  private KustomizeConfig kustomizeConfig;
  @Nullable private HelmCommandFlag helmCommandFlag;

  private Boolean pollForChanges;
  @Transient private String serviceName;
  private enum ManifestCollectionStatus { UNSTABLE, COLLECTING, STABLE }
  private ManifestCollectionStatus collectionStatus;
  private String perpetualTaskId;
  private int failedAttempts;

  public ApplicationManifest cloneInternal() {
    ApplicationManifest manifest = ApplicationManifest.builder()
                                       .accountId(this.accountId)
                                       .serviceId(this.serviceId)
                                       .envId(this.envId)
                                       .storeType(this.storeType)
                                       .gitFileConfig(this.gitFileConfig)
                                       .kind(this.kind)
                                       .helmChartConfig(helmChartConfig)
                                       .kustomizeConfig(KustomizeConfig.cloneFrom(this.kustomizeConfig))
                                       .pollForChanges(this.pollForChanges)
                                       .build();
    manifest.setAppId(this.appId);
    return manifest;
  }

  public enum AppManifestSource { SERVICE, ENV, ENV_SERVICE }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  public static final class Yaml extends BaseEntityYaml {
    private String storeType;
    private GitFileConfig gitFileConfig;
    private HelmChartConfig helmChartConfig;
    private KustomizeConfig kustomizeConfig;
    private Boolean pollForChanges;
    private HelmCommandFlag helmCommandFlag;

    @Builder
    public Yaml(String type, String harnessApiVersion, String storeType, GitFileConfig gitFileConfig,
        HelmChartConfig helmChartConfig, KustomizeConfig kustomizeConfig, Boolean pollForChanges,
        HelmCommandFlag helmCommandFlag) {
      super(type, harnessApiVersion);
      this.storeType = storeType;
      this.gitFileConfig = gitFileConfig;
      this.helmChartConfig = helmChartConfig;
      this.kustomizeConfig = kustomizeConfig;
      this.pollForChanges = pollForChanges;
      this.helmCommandFlag = helmCommandFlag;
    }
  }
}
