package io.harness.callgraph.util;

import java.net.URL;
import java.security.ProtectionDomain;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.utility.JavaModule;

public class TestClassesMatcher implements AgentBuilder.RawMatcher {
  boolean shouldMatch = true;
  public TestClassesMatcher() {}
  public TestClassesMatcher(boolean shouldMatch) {
    this.shouldMatch = shouldMatch;
  }
  @Override
  public boolean matches(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module,
      Class<?> classBeingRedefined, ProtectionDomain protectionDomain) {
    if (protectionDomain != null && protectionDomain.getCodeSource() != null
        && protectionDomain.getCodeSource().getLocation() != null) {
      URL location = protectionDomain.getCodeSource().getLocation();
      return shouldMatch
          && ("file".equals(location.getProtocol()) && location.getPath().endsWith("/target/test-classes/"));
    }
    return !shouldMatch;
  }
}