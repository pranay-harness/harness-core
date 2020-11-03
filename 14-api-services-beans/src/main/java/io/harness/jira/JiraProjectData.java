package io.harness.jira;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.annotations.dev.OwnedBy;
import lombok.Data;
import net.rcarz.jiraclient.IssueType;
import net.rcarz.jiraclient.Project;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static io.harness.annotations.dev.HarnessTeam.CDC;

@OwnedBy(CDC)
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

  public JiraProjectData(Project project) {
    this.id = project.getId();
    this.key = project.getKey();
    this.name = project.getName();
    List<IssueType> issueTypes = project.getIssueTypes();
    for (IssueType issueType : issueTypes) {
      this.issueTypes.add(new JiraIssueType(issueType));
    }
  }
}
