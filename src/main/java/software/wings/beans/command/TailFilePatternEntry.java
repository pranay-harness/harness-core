package software.wings.beans.command;

import com.github.reinert.jjschema.Attributes;
import software.wings.stencils.DefaultValue;

/**
 * Created by peeyushaggarwal on 8/3/16.
 */
public class TailFilePatternEntry {
  @DefaultValue("\"$WINGS_RUNTIME_PATH\"/") @Attributes(title = "File to tail") private String filePath;
  @Attributes(title = "Pattern to search") private String pattern;

  public String getFilePath() {
    return filePath;
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath;
  }

  public String getPattern() {
    return pattern;
  }

  public void setPattern(String pattern) {
    this.pattern = pattern;
  }

  public static final class Builder {
    private String filePath;
    private String pattern;

    private Builder() {}

    public static Builder aTailFilePatternEntry() {
      return new Builder();
    }

    public Builder withFilePath(String filePath) {
      this.filePath = filePath;
      return this;
    }

    public Builder withPattern(String pattern) {
      this.pattern = pattern;
      return this;
    }

    public Builder but() {
      return aTailFilePatternEntry().withFilePath(filePath).withPattern(pattern);
    }

    public TailFilePatternEntry build() {
      TailFilePatternEntry tailFilePatternEntry = new TailFilePatternEntry();
      tailFilePatternEntry.setFilePath(filePath);
      tailFilePatternEntry.setPattern(pattern);
      return tailFilePatternEntry;
    }
  }
}
