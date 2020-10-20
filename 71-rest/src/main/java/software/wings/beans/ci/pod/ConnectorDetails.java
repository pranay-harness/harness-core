package software.wings.beans.ci.pod;

import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import javax.validation.constraints.NotNull;

@Value
@Builder
public class ConnectorDetails {
  @NotNull private ConnectorDTO connectorDTO;
  @NotNull private List<EncryptedDataDetail> encryptedDataDetails;
}
