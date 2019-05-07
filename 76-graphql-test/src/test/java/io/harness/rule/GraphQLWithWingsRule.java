package io.harness.rule;

import com.google.inject.Module;

import com.deftlabs.lock.mongo.DistributedLockSvc;
import io.dropwizard.Configuration;
import software.wings.app.GraphQLModule;
import software.wings.rules.WingsRule;

import java.util.List;

public class GraphQLWithWingsRule extends WingsRule {
  @Override
  protected List<Module> getRequiredModules(Configuration configuration, DistributedLockSvc distributedLockSvc) {
    List<Module> modules = super.getRequiredModules(configuration, distributedLockSvc);
    modules.add(new GraphQLModule());
    return modules;
  }
}
