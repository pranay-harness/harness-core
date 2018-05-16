package software.wings.beans.container;

import com.github.reinert.jjschema.Attributes;
import lombok.Data;

@Data
public class Label {
  @Attributes(title = "Name") private String name;
  @Attributes(title = "Value") private String value;

  public static final class Builder {
    private String name;
    private String value;

    private Builder() {}

    public static Builder aLabel() {
      return new Builder();
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withValue(String value) {
      this.value = value;
      return this;
    }

    public Label build() {
      Label label = new Label();
      label.setName(name);
      label.setValue(value);
      return label;
    }
  }
}