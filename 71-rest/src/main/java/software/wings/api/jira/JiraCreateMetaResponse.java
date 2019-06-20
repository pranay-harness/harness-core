package software.wings.api.jira;

import lombok.Data;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.codehaus.jackson.annotate.JsonTypeName;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonTypeName("jiraCreateMeta")
public class JiraCreateMetaResponse {
  private String expand;
  private List<JiraProjectData> projects = new ArrayList<>();

  public JiraCreateMetaResponse(JSONObject response) {
    this.expand = response.getString("expand");
    JSONArray projectList = response.getJSONArray("projects");
    for (int i = 0; i < projectList.size(); i++) {
      JSONObject proj = projectList.getJSONObject(i);
      this.projects.add(new JiraProjectData(proj));
    }
  }
}
