package io.harness.app.beans.entities;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.harness.ng.core.common.beans.NGTag;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_EMPTY)
public class CIBuildPipeline {
  String id;
  String name;
  // tags implementation might change. Refrain from using it.
  List<NGTag> tags;
}
