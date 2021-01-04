package software.wings.beans.appmanifest;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.Trimmed;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.Field;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.NameAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import software.wings.beans.entityinterface.ApplicationAccess;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@OwnedBy(HarnessTeam.CDC)
@CdIndex(name = "appId_serviceId", fields = { @Field("appId")
                                              , @Field("serviceId") })
@CdIndex(name = "account_appManifestId", fields = { @Field("accountId")
                                                    , @Field("applicationManifestId") })
@CdIndex(name = "appId_appManifestId", fields = { @Field("appId")
                                                  , @Field("applicationManifestId") })
@Data
@Builder
@Entity(value = "helmCharts", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "HelmChartKeys")
public class HelmChart implements AccountAccess, NameAccess, PersistentEntity, UuidAware, CreatedAtAware,
                                  UpdatedAtAware, ApplicationAccess {
  @Id private String uuid;
  @Trimmed private String version;
  @FdIndex private String applicationManifestId;
  private String name;
  private String displayName;
  @FdIndex private String accountId;
  private String appId;
  private String serviceId;
  private long createdAt;
  private long lastUpdatedAt;
  private String appVersion;
  private String description;
  private Map<String, String> metadata;
}
