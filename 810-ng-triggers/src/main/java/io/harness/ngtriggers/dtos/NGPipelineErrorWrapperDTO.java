/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ngtriggers.dtos;

import io.swagger.annotations.ApiModel;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@ApiModel("NGPipelineErrorWrapper")
public class NGPipelineErrorWrapperDTO {
  String errorPipelineYaml;
  Map<String, NGPipelineErrorResponseDTO> uuidToErrorResponseMap;
}
