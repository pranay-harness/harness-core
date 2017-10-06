package software.wings.beans;

import com.github.reinert.jjschema.Attributes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LambdaTestEvent {
  @Attributes(title = "Function Name") private String functionName;
  @Attributes(title = "Payload") private String payload;
  @Attributes(title = "Assertion") private String assertion;
  @Attributes(title = "Disable Test") private String disable;
}
