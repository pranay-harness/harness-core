package io.harness.rule;

import io.harness.app.GraphQLModule;

import software.wings.rules.WingsRule;

import com.google.inject.Module;
import java.lang.annotation.Annotation;
import java.util.List;

public class GraphQLWithWingsRule extends WingsRule {
  @Override
  public List<Module> modules(List<Annotation> annotations) throws Exception {
    List<Module> modules = super.modules(annotations);
    modules.add(GraphQLModule.getInstance());
    return modules;
  }
}
