package io.harness.pms.ngpipeline.inputset.beans.resource;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
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
public class MergeInputSetRequestDTOPMS {
  @NotEmpty List<String> inputSetReferences;
  boolean withMergedPipelineYaml;
  List<String> stageIdentifiers;
}
