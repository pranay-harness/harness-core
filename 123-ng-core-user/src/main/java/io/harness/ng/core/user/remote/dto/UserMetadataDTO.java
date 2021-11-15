package io.harness.ng.core.user.remote.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(PL)
@Schema(name = "UserMetadata", description = "This is the view of the UserMetadata entity defined in Harness")
public class UserMetadataDTO {
  String name;
  @ApiModelProperty(required = true) @NotEmpty String email;
  @ApiModelProperty(required = true) @NotEmpty String uuid;
  @NotEmpty boolean locked;

  @Builder
  public UserMetadataDTO(String name, String email, String uuid, boolean locked) {
    this.name = name;
    this.email = email;
    this.uuid = uuid;
    this.locked = locked;
  }
}
