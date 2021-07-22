package io.harness.metadata;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.CastedField;
import io.harness.beans.RecasterMap;
import io.harness.exceptions.RecastMetadataException;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public class RecastMetadataProcessor {
  public static final String METADATA = "__metadata";
  public static final String ANNOTATIONS = "__annotations";

  public void populateAnnotationMetadata(RecasterMap recasterMap, CastedField cf) {
    createMetadataConstructionIfNeeded(recasterMap);
    createAnnotationConstructionIfNeeded(recasterMap);
    Map<String, List<Annotation>> annotations = cf.getAnnotationsMan();
    String nameOfTheField = cf.getNameToStore();
    ((RecasterMap) ((RecasterMap) recasterMap.get(METADATA)).get(ANNOTATIONS))
        .put(nameOfTheField, annotations.get(nameOfTheField));
  }

  private void createMetadataConstructionIfNeeded(RecasterMap recasterMap) {
    if (recasterMap.containsKey(METADATA)) {
      return;
    }
    recasterMap.put(METADATA, new RecasterMap());
  }

  private void createAnnotationConstructionIfNeeded(RecasterMap recasterMap) {
    if (!recasterMap.containsKey(METADATA)) {
      throw new RecastMetadataException(METADATA + " should present in recasterMap!!!");
    }

    RecasterMap metadata = (RecasterMap) recasterMap.get(METADATA);

    if (recasterMap.containsKey(ANNOTATIONS)) {
      return;
    }

    metadata.put(ANNOTATIONS, new RecasterMap());
  }
}
