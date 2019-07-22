package software.wings.infra.data;

import lombok.AllArgsConstructor;
import software.wings.annotation.CustomFieldMapKey;
import software.wings.annotation.ExcludeFieldMap;
import software.wings.infra.FieldKeyValMapProvider;

@AllArgsConstructor
public class DummyPerson implements FieldKeyValMapProvider {
  @CustomFieldMapKey("customKey") private String id;
  private String name;
  @ExcludeFieldMap private String occupation;
}