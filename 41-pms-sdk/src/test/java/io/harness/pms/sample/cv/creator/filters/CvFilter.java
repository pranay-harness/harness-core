package io.harness.pms.sample.cv.creator.filters;

import io.harness.pms.filter.PipelineFilter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(Include.NON_NULL)
public class CvFilter implements PipelineFilter {}
