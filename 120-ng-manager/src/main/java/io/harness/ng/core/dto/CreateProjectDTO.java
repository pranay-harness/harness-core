package io.harness.ng.core.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import io.harness.data.validator.Trimmed;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.Size;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateProjectDTO {
  @Trimmed @NotEmpty String accountIdentifier;
  @NotEmpty @EntityIdentifier String identifier;
  @NotEmpty @EntityName String name;
  @Trimmed @NotEmpty String color;
  @Size(max = 1024) List<String> purposeList;
  @Size(max = 1024) String description = "";
  @Size(min = 1, max = 128) List<String> owners;
  @Size(max = 128) List<String> tags = new ArrayList<>();
}
