package software.wings.settings.validation;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonTypeName("WINRM_CONNECTION_ATTRIBUTES")
@OwnedBy(CDP)
public class WinRmConnectivityValidationAttributes extends ConnectivityValidationAttributes {
  @NotEmpty private String hostName;
}
