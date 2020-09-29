package io.harness.cdng.inputset.beans.resource;

import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Value
@Builder
@ApiModel("MergeInputSetErrorWrapper")
public class MergeInputSetErrorWrapperDTO {
  @Builder.Default List<MergeInputSetErrorDTO> errors = new ArrayList<>();
}
