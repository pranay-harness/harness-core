package software.wings.beans.ce;

import io.harness.annotation.StoreIn;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(value = "ceMetadataRecord", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "CEMetadataRecordKeys")
@StoreIn("events")
public class CEMetadataRecord implements PersistentEntity, UuidAware, AccountAccess, UpdatedAtAware {
  @Id private String uuid;
  private String accountId;
  private Boolean clusterDataConfigured;
  private Boolean awsConnectorConfigured;
  private Boolean gcpConnectorConfigured;
  private long lastUpdatedAt;
}
