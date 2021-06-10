package io.harness.pms.tags;

import static io.harness.pms.yaml.YamlNode.UUID_FIELD_NAME;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;

import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PIPELINE)
@UtilityClass
public class TagUtils {
  public void removeUuidFromTags(Map<String, String> tags) {
    if (EmptyPredicate.isNotEmpty(tags)) {
      tags.remove(UUID_FIELD_NAME);
    }
  }
}
