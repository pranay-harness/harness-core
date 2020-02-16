package io.harness.rule;

import com.google.inject.Module;

import com.mongodb.MongoClient;
import io.dropwizard.Configuration;
import software.wings.app.GraphQLModule;
import software.wings.rules.WingsRule;

import java.util.List;

public class GraphQLWithWingsRule extends WingsRule {
  @Override
  protected List<Module> getRequiredModules(
      Configuration configuration, MongoClient locksMongoClient, String locksDatabase) {
    List<Module> modules = super.getRequiredModules(configuration, locksMongoClient, locksDatabase);
    modules.add(new GraphQLModule());
    return modules;
  }
}
