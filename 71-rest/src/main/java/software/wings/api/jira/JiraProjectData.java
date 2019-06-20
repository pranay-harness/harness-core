package software.wings.api.jira;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

@Data
public class JiraProjectData {
  private String id;
  private String key;
  private String name;
  @JsonProperty("issuetypes") private List<JiraIssueType> issueTypes = new ArrayList<>();

  public JiraProjectData(JSONObject obj) {
    this.id = obj.getString("id");
    this.key = obj.getString("key");
    this.name = obj.getString("name");
    JSONArray issueTypeList = obj.getJSONArray("issuetypes");
    for (int i = 0; i < issueTypeList.size(); i++) {
      JSONObject issueTypeObj = issueTypeList.getJSONObject(i);
      this.issueTypes.add(new JiraIssueType(issueTypeObj));
    }
  }
}
