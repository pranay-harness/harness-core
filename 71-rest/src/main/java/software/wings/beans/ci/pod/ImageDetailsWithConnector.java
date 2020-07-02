package software.wings.beans.ci.pod;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.container.ImageDetails;

import java.util.List;
import javax.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageDetailsWithConnector {
  @NotNull private String connectorName;
  @NotNull private EncryptableSetting encryptableSetting;
  @NotNull private List<EncryptedDataDetail> encryptedDataDetails;
  @NotNull private ImageDetails imageDetails;
}
