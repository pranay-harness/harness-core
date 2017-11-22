package software.wings.beans.container;

import com.github.reinert.jjschema.Attributes;
import lombok.Data;

@Data
public class Label {
  @Attributes(title = "Label name") private String name;
  @Attributes(title = "Label value") private String value;
}