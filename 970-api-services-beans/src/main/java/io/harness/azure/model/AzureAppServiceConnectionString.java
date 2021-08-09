package io.harness.azure.model;

import static io.harness.expression.Expression.ALLOW_SECRETS;

import io.harness.expression.Expression;
import io.harness.expression.ExpressionReflectionUtils.NestedAnnotationResolver;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureAppServiceConnectionString implements NestedAnnotationResolver {
  private String name;
  @Expression(ALLOW_SECRETS) private String value;
  private AzureAppServiceConnectionStringType type;

  @JsonProperty(value = "slotSetting") private boolean sticky;
}
