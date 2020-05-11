package software.wings.settings.validation;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(CDC)
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonTypeName("SLACK")
public class SlackConnectivityValidationAttributes extends ConnectivityValidationAttributes {
  @NotEmpty private String channel;
  private String message;
  private String sender;
}