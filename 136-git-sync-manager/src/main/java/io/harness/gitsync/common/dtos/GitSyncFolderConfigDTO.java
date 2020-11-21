package io.harness.gitsync.common.dtos;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.Trimmed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class GitSyncFolderConfigDTO {
  @Trimmed @NotEmpty private String rootFolder;
  private Boolean isDefault;
  @NotEmpty @EntityIdentifier private String identifier;
  private Boolean enabled;
}
