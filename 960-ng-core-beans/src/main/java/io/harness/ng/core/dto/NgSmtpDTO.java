package io.harness.ng.core.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.Trimmed;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(PL)
@Data
@Builder
@Schema(name = "NgSmtp", description = "This is the view of the NgSmtp entity defined in Harness")
public class NgSmtpDTO {
  private String uuid;
  @NotEmpty String accountId;
  @NotNull @NotBlank @Trimmed(message = "The name must not have trailing spaces.") private String name;
  @Valid private SmtpConfigDTO value;
}
