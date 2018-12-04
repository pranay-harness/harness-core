package software.wings.settings.validation;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonTypeName("SLACK")
public class SlackConnectivityValidationAttributes extends ConnectivityValidationAttributes {
  @NotEmpty private String channel;
  private String message;
  private String sender;
}