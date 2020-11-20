package software.wings.infra.data;

import lombok.AllArgsConstructor;
import software.wings.annotation.CustomFieldMapKey;
import software.wings.annotation.IncludeFieldMap;
import software.wings.infra.FieldKeyValMapProvider;

@AllArgsConstructor
public class DummyPerson implements FieldKeyValMapProvider {
  @IncludeFieldMap @CustomFieldMapKey("customKey") private String id;
  @IncludeFieldMap private String name;
  private String occupation;
}
