package software.wings.beans.marketplace.gcp;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.DbAliases;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@OwnedBy(PL)
@FieldNameConstants(innerTypeName = "GCPMarketplaceCustomerKeys")
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@Entity(value = "gcpMarketplaceCustomers", noClassnameStored = true)
@StoreIn(DbAliases.CG_MANAGER)
public final class GCPMarketplaceCustomer implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware {
  @Id private String uuid;

  private final String gcpAccountId;
  private final String harnessAccountId;
  private List<GCPMarketplaceProduct> products;

  private long createdAt;
  private long lastUpdatedAt;
}
