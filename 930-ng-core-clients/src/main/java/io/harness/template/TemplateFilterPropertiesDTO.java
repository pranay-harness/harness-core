package io.harness.template;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.filter.FilterConstants.TEMPLATE_FILTER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.filter.FilterType;
import io.harness.filter.dto.FilterPropertiesDTO;
import io.harness.ng.core.template.TemplateEntityType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel("TemplateFilterProperties")
@Schema(name = "TemplateFilterProperties", description = "This contains details of the Template Filter")
@JsonTypeName(TEMPLATE_FILTER)
@OwnedBy(CDC)
public class TemplateFilterPropertiesDTO extends FilterPropertiesDTO {
  List<String> templateNames;
  List<String> templateIdentifiers;
  String description;
  List<TemplateEntityType> templateEntityTypes;
  List<String> childTypes;

  @Override
  public FilterType getFilterType() {
    return FilterType.TEMPLATE;
  }
}
