package io.harness.template.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@OwnedBy(CDC)
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel("TemplateApplyRequest")
public class TemplateApplyRequestDTO {
  // This could be original pipeline yaml, or any other entity on which we are referring templateRef.
  @NotNull String originalEntityYaml;
  List<String> inputSetYamlList;
}
