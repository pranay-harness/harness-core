package io.harness.plan.input;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.serializer.KryoUtils;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Redesign
@EqualsAndHashCode
public class InputArgs {
  private final Map<String, String> strMap;
  private final Map<String, byte[]> bytesMap;

  public InputArgs() {
    this.strMap = new HashMap<>();
    this.bytesMap = new HashMap<>();
  }

  private InputArgs(Map<String, String> strMap, Map<String, byte[]> bytesMap) {
    this.strMap = strMap;
    this.bytesMap = bytesMap;
  }

  public boolean containsKey(String key) {
    return strMap.containsKey(key) || bytesMap.containsKey(key);
  }

  public Object get(String key) {
    String str = strMap.get(key);
    if (str != null) {
      return str;
    }

    byte[] bs = bytesMap.get(key);
    if (bs == null) {
      return null;
    }

    return KryoUtils.asObject(bs);
  }

  public Set<String> keySet() {
    HashSet<String> keySet = new HashSet<>(strMap.keySet());
    keySet.addAll(bytesMap.keySet());
    return keySet;
  }

  /**
   * strMap is the map of all entries inside InputArgs which have String value.
   */
  @NotNull
  public Map<String, String> strMap() {
    return new HashMap<>(strMap);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private final Map<String, String> strMap = new HashMap<>();
    private final Map<String, byte[]> bytesMap = new HashMap<>();

    /**
     * Add key-value to the input args.
     *
     * NOTE: The value should be Kryo serializable.
     */
    public Builder put(String key, Object value) {
      if (value instanceof String) {
        bytesMap.remove(key);
        strMap.put(key, (String) value);
      } else if (value != null) {
        strMap.remove(key);
        bytesMap.put(key, KryoUtils.asBytes(value));
      }
      return this;
    }

    /**
     * Adds all key-value pairs to the input args.
     *
     * NOTE: The values should be Kryo serializable.
     */
    public Builder putAll(Map<String, Object> other) {
      if (other != null) {
        other.forEach(this ::put);
      }
      return this;
    }

    public InputArgs build() {
      return new InputArgs(strMap, bytesMap);
    }
  }
}
