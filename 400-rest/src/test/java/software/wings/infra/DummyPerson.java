package software.wings.infra;

import software.wings.annotation.CustomFieldMapKey;
import software.wings.annotation.IncludeFieldMap;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DummyPerson implements FieldKeyValMapProvider {
  @IncludeFieldMap @CustomFieldMapKey("customKey") private String id;
  @IncludeFieldMap private String name;
  private String occupation;
}
