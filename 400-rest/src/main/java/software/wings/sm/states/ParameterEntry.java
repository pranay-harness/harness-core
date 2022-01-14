package software.wings.sm.states;

import com.github.reinert.jjschema.Attributes;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by sgurubelli on 8/31/17.
 */
@Data
@NoArgsConstructor
public class ParameterEntry {
  @Attributes(title = "Name") String key;
  @Attributes(title = "Value") String value;
}
