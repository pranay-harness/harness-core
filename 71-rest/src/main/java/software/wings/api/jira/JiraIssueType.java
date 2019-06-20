package software.wings.api.jira;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import net.sf.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import javax.validation.constraints.NotNull;

@Data
public class JiraIssueType {
  @NotNull private String id;
  @NotNull private String name;
  @NotNull private boolean isSubTask;
  @NotNull private String description;
  @JsonProperty("fields") private Map<String, JiraField> jiraFields = new HashMap<>();

  JiraIssueType(JSONObject data) {
    this.id = data.getString("id");
    this.name = data.getString("name");
    this.description = data.getString("description");
    this.isSubTask = data.getBoolean("subtask");
    JSONObject fields = data.getJSONObject("fields");
    fields.keySet().forEach(keyStr -> {
      String kk = (String) keyStr;
      JSONObject fieldData = fields.getJSONObject(kk);
      this.jiraFields.put(kk, new JiraField(fieldData, kk));
    });
  }
}
