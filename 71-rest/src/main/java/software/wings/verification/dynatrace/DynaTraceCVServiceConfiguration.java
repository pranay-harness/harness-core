package software.wings.verification.dynatrace;

import com.github.reinert.jjschema.Attributes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.verification.CVConfiguration;

/**
 * Created by Pranjal on 10/16/2018
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DynaTraceCVServiceConfiguration extends CVConfiguration {
  @Attributes(required = true, title = "Service Methods") private String serviceMethods;
}
