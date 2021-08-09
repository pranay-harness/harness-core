package io.harness.jira;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import net.sf.json.JSONObject;

@Data
@FieldNameConstants(innerTypeName = "JiraStatusCategoryKeys")
public class JiraStatusCategory {
  private String id;
  private String key;
  private String name;

  public JiraStatusCategory(JSONObject jsonObject) {
    this.id = jsonObject.getString(JiraStatusCategoryKeys.id);
    this.key = jsonObject.getString(JiraStatusCategoryKeys.key);
    this.name = jsonObject.getString(JiraStatusCategoryKeys.name);
  }
}
