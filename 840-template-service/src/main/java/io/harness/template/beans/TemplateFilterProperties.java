package io.harness.template.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.filter.FilterConstants.TEMPLATE_FILTER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.filter.entity.FilterProperties;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@TypeAlias("TemplateFilterPropertiesEntity")
@JsonTypeName(TEMPLATE_FILTER)
@OwnedBy(CDC)
public class TemplateFilterProperties extends FilterProperties {
  List<String> templateNames;
  List<String> templateIdentifiers;
  String description;
  List<TemplateEntityType> templateEntityTypes;
  List<String> childTypes;
}
