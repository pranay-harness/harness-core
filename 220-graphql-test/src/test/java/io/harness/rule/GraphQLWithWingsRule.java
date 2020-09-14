package io.harness.rule;

import com.google.inject.Module;

import software.wings.app.GraphQLModule;
import software.wings.rules.WingsRule;

import java.lang.annotation.Annotation;
import java.util.List;

public class GraphQLWithWingsRule extends WingsRule {
  @Override
  public List<Module> modules(List<Annotation> annotations) throws Exception {
    List<Module> modules = super.modules(annotations);
    modules.add(new GraphQLModule());
    return modules;
  }
}
