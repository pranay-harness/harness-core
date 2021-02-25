package io.harness.accesscontrol.clients;

import io.harness.accesscontrol.Principal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccessCheckRequestDTO {
  @NotEmpty @Size(max = 1000) List<PermissionCheckDTO> permissions;
  @NotNull @Valid Principal principal;
}
