package software.wings.beans.command;

import com.github.reinert.jjschema.Attributes;
import software.wings.stencils.DefaultValue;

/**
 * Created by peeyushaggarwal on 8/3/16.
 */
public class TailFilePatternEntry {
  @DefaultValue("\"$WINGS_RUNTIME_PATH\"/") @Attributes(title = "File to tail") private String filePath;
  @Attributes(title = "Pattern to search") private String pattern;

  /**
   * Gets file path.
   *
   * @return the file path
   */
  public String getFilePath() {
    return filePath;
  }

  /**
   * Sets file path.
   *
   * @param filePath the file path
   */
  public void setFilePath(String filePath) {
    this.filePath = filePath;
  }

  /**
   * Gets pattern.
   *
   * @return the pattern
   */
  public String getPattern() {
    return pattern;
  }

  /**
   * Sets pattern.
   *
   * @param pattern the pattern
   */
  public void setPattern(String pattern) {
    this.pattern = pattern;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String filePath;
    private String pattern;

    private Builder() {}

    /**
     * A tail file pattern entry builder.
     *
     * @return the builder
     */
    public static Builder aTailFilePatternEntry() {
      return new Builder();
    }

    /**
     * With file path builder.
     *
     * @param filePath the file path
     * @return the builder
     */
    public Builder withFilePath(String filePath) {
      this.filePath = filePath;
      return this;
    }

    /**
     * With pattern builder.
     *
     * @param pattern the pattern
     * @return the builder
     */
    public Builder withPattern(String pattern) {
      this.pattern = pattern;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aTailFilePatternEntry().withFilePath(filePath).withPattern(pattern);
    }

    /**
     * Build tail file pattern entry.
     *
     * @return the tail file pattern entry
     */
    public TailFilePatternEntry build() {
      TailFilePatternEntry tailFilePatternEntry = new TailFilePatternEntry();
      tailFilePatternEntry.setFilePath(filePath);
      tailFilePatternEntry.setPattern(pattern);
      return tailFilePatternEntry;
    }
  }
}
