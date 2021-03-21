package io.harness.connector.entities.embedded.ceazure;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.Connector;
import io.harness.delegate.beans.connector.ceazure.CEAzureFeatures;

import java.util.List;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
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
@FieldNameConstants(innerTypeName = "CEAzureConfigKeys")
@Entity(value = "connectors", noClassnameStored = true)
@TypeAlias("io.harness.connector.entities.embedded.ceazure.CEAzureConfig")
@OwnedBy(DX)
public class CEAzureConfig extends Connector {
  @NotEmpty List<CEAzureFeatures> featuresEnabled;
  @NotNull String subscriptionId;
  @NotNull String tenantId;
  @Nullable BillingExportDetails billingExportDetails;
}
