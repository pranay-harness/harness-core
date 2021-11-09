package io.harness.serviceaccount;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.EntityIdentifier;

import io.swagger.annotations.ApiModelProperty;
import java.util.Map;
import javax.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(PL)
public class ServiceAccountDTO {
  @ApiModelProperty(required = true) @EntityIdentifier @NotBlank String identifier;
  @ApiModelProperty(required = true) @NotBlank String name;
  @ApiModelProperty(required = true) @Email @NotBlank String email;
  @Size(max = 1024) String description;
  @Size(max = 128) Map<String, String> tags;
  @ApiModelProperty(required = true) @NotBlank String accountIdentifier;
  @EntityIdentifier(allowBlank = true) String orgIdentifier;
  @EntityIdentifier(allowBlank = true) String projectIdentifier;
}
