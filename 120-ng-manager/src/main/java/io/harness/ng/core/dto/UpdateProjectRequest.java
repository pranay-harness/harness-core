package io.harness.ng.core.dto;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.harness.data.validator.EntityName;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;
import javax.validation.constraints.Size;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(NON_NULL)
public class UpdateProjectRequest {
  @NotEmpty @EntityName String name;
  @Size(max = 1024) String description;
  @Size(min = 1, max = 128) List<String> owners;
  @Size(max = 128) List<String> tags;
}
