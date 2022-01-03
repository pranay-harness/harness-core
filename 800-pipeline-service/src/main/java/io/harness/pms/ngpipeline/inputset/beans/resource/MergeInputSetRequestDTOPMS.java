package io.harness.pms.ngpipeline.inputset.beans.resource;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(PIPELINE)
@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel("MergeInputSetRequest")
@Schema(name = "MergeInputSetRequest", description = "Contains list of Input Set references and Stage Ids")
public class MergeInputSetRequestDTOPMS {
  @Schema(description = "List of Input Set References to be merged") @NotEmpty List<String> inputSetReferences;
  @Schema(
      description =
          "This is a boolean value that indicates if the response must contain the YAML for the merged Pipeline. The default value is False.")
  boolean withMergedPipelineYaml;
  @Schema(description = "List of Stage Ids. Input Sets corresponding to these Ids will be merged.")
  List<String> stageIdentifiers;
}
