package software.wings.api.jira;

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

  JiraField(JSONObject obj, String keyStr) {
    this.required = obj.getBoolean("required");
    this.name = obj.getString("name");
    this.key = obj.get("key") == null ? keyStr : obj.getString("key");
    this.schema = obj.getJSONObject("schema");
    if (obj.keySet().contains("allowedValues")) {
      this.allowedValues = obj.getJSONArray("allowedValues");
    }
    if (this.key.startsWith("customfield_")) {
      this.isCustom = true;
    }
  }
}
