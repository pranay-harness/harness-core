package io.harness.delegate.beans.connector.gcpccm;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import java.util.List;
import javax.validation.Valid;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel("GcpCloudCostConnector")
@OwnedBy(CE)
public class GcpCloudCostConnectorDTO extends ConnectorConfigDTO {
  @Valid
  @NotEmpty(message = "At least one GcpCcmFeatures should be enabled")
  List<GcpCloudCostFeatures> featuresEnabled;
  @Valid GcpBillingExportSpecDTO billingExportSpec;

  @Override
  public List<DecryptableEntity> getDecryptableEntities() {
    return null;
  }
}
