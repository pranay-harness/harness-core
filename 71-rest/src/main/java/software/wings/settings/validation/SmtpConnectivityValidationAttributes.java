package software.wings.settings.validation;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonTypeName("SMTP")
public class SmtpConnectivityValidationAttributes extends ConnectivityValidationAttributes {
  public static final String DEFAULT_TEXT = "Harness Inc e-mail for test Smtp connectivity";
  @NotEmpty private String to;
  private String body;
  private String subject;
}