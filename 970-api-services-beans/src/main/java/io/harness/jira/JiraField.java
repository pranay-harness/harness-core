package io.harness.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Data;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Data
public class JiraField {
  @NotNull private String key;
  @NotNull private String name;
  @NotNull private boolean required;
  @NotNull private boolean isCustom;
  @NotNull private JSONObject schema;
  private JSONArray allowedValues;

  public JiraField(JSONObject obj, String keyStr) {
    this.required = obj.getBoolean("required");
    this.name = obj.getString("name");
    this.key = obj.get("key") == null ? keyStr : obj.getString("key");
    this.schema = obj.getJSONObject("schema");
    if (obj.containsKey("allowedValues")) {
      this.allowedValues = obj.getJSONArray("allowedValues");
    }
    if (this.key.startsWith("customfield_")) {
      this.isCustom = true;
    }
  }

  public static JiraField getNewField(JSONObject object, String key) {
    return new JiraField(object, key);
  }
}
