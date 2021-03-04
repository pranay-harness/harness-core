package software.wings.beans;

import static io.harness.expression.Expression.ALLOW_SECRETS;

import io.harness.data.validator.Trimmed;
import io.harness.expression.Expression;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Generic Name Value pair
 * @author rktummala on 10/27/17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "NameValuePairKeys")
public class NameValuePair {
  @NotEmpty @Trimmed private String name;
  /*
    Value can only be of type String or in encrypted format
  */
  @NotNull @Expression(ALLOW_SECRETS) private String value;

  /*
   Could be TEXT / ENCRYPTED_TEXT
   TODO: Why is this not an enum? @swagat
  */
  private String valueType;

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static class Yaml extends NameValuePairAbstractYaml {
    @Builder
    public Yaml(String name, String value, String valueType, List<AllowedValueYaml> allowedValueYamlList) {
      super(name, value, valueType, allowedValueYamlList);
    }
  }
}
