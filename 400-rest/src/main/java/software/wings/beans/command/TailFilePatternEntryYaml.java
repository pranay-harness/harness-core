package software.wings.beans.command;

import io.harness.yaml.BaseYaml;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TailFilePatternEntryYaml extends BaseYaml {
  private String filePath;
  // maps to pattern
  private String searchPattern;

  public static final class Builder {
    private String filePath;
    // maps to pattern
    private String searchPattern;

    private Builder() {}

    public static Builder anYaml() {
      return new Builder();
    }

    public Builder withFilePath(String filePath) {
      this.filePath = filePath;
      return this;
    }

    public Builder withSearchPattern(String searchPattern) {
      this.searchPattern = searchPattern;
      return this;
    }

    public Builder but() {
      return anYaml().withFilePath(filePath).withSearchPattern(searchPattern);
    }

    public TailFilePatternEntryYaml build() {
      TailFilePatternEntryYaml yaml = new TailFilePatternEntryYaml();
      yaml.setFilePath(filePath);
      yaml.setSearchPattern(searchPattern);
      return yaml;
    }
  }
}
