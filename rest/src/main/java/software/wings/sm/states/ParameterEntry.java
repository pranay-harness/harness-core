package software.wings.sm.states;

import com.github.reinert.jjschema.Attributes;

/**
 * Created by sgurubelli on 8/31/17.
 */
public class ParameterEntry {
  @Attributes(title = "Name") String key;
  @Attributes(title = "Value") String value;

  /**
   * Getter for property 'filePath'.
   *
   * @return Value for property 'filePath'.
   */
  public String getKey() {
    return key;
  }

  /**
   * Setter for property 'filePath'.
   *
   * @param key Value to set for property 'filePath'.
   */
  public void setKey(String key) {
    this.key = key;
  }

  /**
   * Getter for property 'assertion'.
   *
   * @return Value for property 'assertion'.
   */
  public String getValue() {
    return value;
  }

  /**
   * Setter for property 'assertion'.
   *
   * @param value Value to set for property 'assertion'.
   */
  public void setValue(String value) {
    this.value = value;
  }
}
