/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.jira;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import net.sf.json.JSONObject;

@Data
@FieldNameConstants(innerTypeName = "JiraStatusKeys")
public class JiraStatus {
  private String id;
  private String name;
  private String untranslatedName;
  private String description;
  private JiraStatusCategory statusCategory;

  public JiraStatus(JSONObject jsonObject) {
    this.id = jsonObject.getString(JiraStatusKeys.id);
    this.name = jsonObject.getString(JiraStatusKeys.name);
    this.untranslatedName = jsonObject.getString(JiraStatusKeys.untranslatedName);
    this.description = jsonObject.getString(JiraStatusKeys.description);
    this.statusCategory = new JiraStatusCategory(jsonObject.getJSONObject(JiraStatusKeys.statusCategory));
  }
}
