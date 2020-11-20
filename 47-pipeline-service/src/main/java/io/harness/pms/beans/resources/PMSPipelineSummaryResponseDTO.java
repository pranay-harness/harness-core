package io.harness.pms.beans.resources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.Version;

import java.util.Map;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel("PMSPipelineSummaryResponse")
public class PMSPipelineSummaryResponseDTO {
  String name;
  String identifier;
  String description;
  Map<String, String> tags;
  @Version Long version;
}
