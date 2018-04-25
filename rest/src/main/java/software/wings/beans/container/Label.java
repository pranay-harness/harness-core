package software.wings.beans.container;

import com.github.reinert.jjschema.Attributes;
import lombok.Data;

@Data
public class Label {
  @Attributes(title = "Name") private String name;
  @Attributes(title = "Value") private String value;
}