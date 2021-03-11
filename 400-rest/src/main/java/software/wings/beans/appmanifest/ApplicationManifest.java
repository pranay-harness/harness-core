package software.wings.beans.appmanifest;

import io.harness.annotation.HarnessEntity;
import io.harness.manifest.CustomSourceConfig;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.AccountAccess;

import software.wings.beans.Base;
import software.wings.beans.GitFileConfig;
import software.wings.beans.HelmChartConfig;
import software.wings.beans.HelmCommandFlagConfig;
import software.wings.helpers.ext.kustomize.KustomizeConfig;

import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Transient;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "ApplicationManifestKeys")
@Entity("applicationManifests")
@HarnessEntity(exportable = true)
public class ApplicationManifest extends Base implements AccountAccess {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("appManifestIdx")
                 .unique(true)
                 .field(BaseKeys.appId)
                 .field(ApplicationManifestKeys.envId)
                 .field(ApplicationManifestKeys.serviceId)
                 .field(ApplicationManifestKeys.kind)
                 .build())
        .build();
  }

  public static final String ID = "_id";

  @FdIndex private String accountId;
  private String serviceId;
  private String envId;
  private AppManifestKind kind;
  @NonNull private StoreType storeType;
  private GitFileConfig gitFileConfig;
  private HelmChartConfig helmChartConfig;
  private KustomizeConfig kustomizeConfig;
  private CustomSourceConfig customSourceConfig;
  @Nullable private HelmCommandFlagConfig helmCommandFlag;

  private Boolean pollForChanges;
  @Transient private String serviceName;
  private enum ManifestCollectionStatus { UNSTABLE, COLLECTING, STABLE }
  private ManifestCollectionStatus collectionStatus;
  private String perpetualTaskId;
  private int failedAttempts;
  private Boolean skipVersioningForAllK8sObjects;

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
                                       .customSourceConfig(CustomSourceConfig.cloneFrom(this.customSourceConfig))
                                       .pollForChanges(this.pollForChanges)
                                       .skipVersioningForAllK8sObjects(this.skipVersioningForAllK8sObjects)
                                       .build();
    manifest.setAppId(this.appId);
    return manifest;
  }

  public enum AppManifestSource { SERVICE, ENV, ENV_SERVICE }
}
