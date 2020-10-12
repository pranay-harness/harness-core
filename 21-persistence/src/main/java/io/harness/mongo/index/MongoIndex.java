package io.harness.mongo.index;

import io.harness.mongo.IndexCreator.IndexCreatorBuilder;

public interface MongoIndex {
  String NAME = "name";
  String BACKGROUND = "background";

  IndexCreatorBuilder createBuilder(String id);
}
