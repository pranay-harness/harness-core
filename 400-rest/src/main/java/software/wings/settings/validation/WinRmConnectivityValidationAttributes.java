package software.wings.settings.validation;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonTypeName("WINRM_CONNECTION_ATTRIBUTES")
public class WinRmConnectivityValidationAttributes extends ConnectivityValidationAttributes {
  @NotEmpty private String hostName;
}
