package io.harness.template.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.EntityName;
import io.harness.encryption.Scope;
import io.harness.gitsync.sdk.EntityGitDetails;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import java.util.Map;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(CDC)
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel("TemplateResponse")
public class TemplateResponseDTO {
  @NotEmpty String accountId;
  String orgIdentifier;
  String projectIdentifier;
  @NotEmpty String identifier;

  @EntityName String name;
  @Size(max = 1024) String description;
  Map<String, String> tags;

  @NotEmpty String yaml;

  String versionLabel;
  boolean isStableTemplate;

  TemplateEntityType templateEntityType;
  String childType;

  Scope templateScope;
  Long version;
  EntityGitDetails gitDetails;
  long lastUpdatedAt;
}
