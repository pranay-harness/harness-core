package io.harness.connector.entities.embedded.gcpccm;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.Connector;
import io.harness.delegate.beans.connector.gcpccm.GcpCloudCostFeatures;

import java.util.List;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@Persistent
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "GcpCloudCostConfigKeys")
@Entity(value = "connectors", noClassnameStored = true)
@TypeAlias("io.harness.connector.entities.embedded.gcpccm.GcpCloudCostConfig")
@OwnedBy(CE)
public class GcpCloudCostConfig extends Connector {
  @NotEmpty List<GcpCloudCostFeatures> featuresEnabled;

  @Nullable GcpBillingExportDetails billingExportDetails;
}
