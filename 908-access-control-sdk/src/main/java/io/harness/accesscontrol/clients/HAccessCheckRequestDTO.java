package io.harness.accesscontrol.clients;

import io.harness.accesscontrol.HPrincipal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import java.util.List;
import javax.annotation.Nullable;
import javax.validation.Valid;
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
@ApiModel(value = "AccessCheckRequest")
public class HAccessCheckRequestDTO {
  @NotEmpty @Size(max = 1000) List<PermissionCheckDTO> permissions;
  @Valid @Nullable HPrincipal principal;
}
