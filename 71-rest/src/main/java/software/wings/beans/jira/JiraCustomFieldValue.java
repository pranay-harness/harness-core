package software.wings.beans.jira;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "JiraCustomFieldValueKeys")
public class JiraCustomFieldValue {
  public String fieldType;
  public String fieldValue;
}
