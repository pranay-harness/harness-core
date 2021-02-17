package io.harness.callgraph.instr;

import io.harness.callgraph.instr.tracer.ConstructorTracer;
import io.harness.callgraph.instr.tracer.MethodTracer;
import io.harness.callgraph.instr.tracer.TestConstructorTracer;
import io.harness.callgraph.instr.tracer.TestMethodTracer;
import io.harness.callgraph.util.StringSetMatcherStartsWith;

import java.lang.instrument.Instrumentation;
import java.util.Set;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.matcher.NameMatcher;
import org.junit.jupiter.api.Test;

/**
 * Instrument the target classes
 */
public class ByteBuddyInstr extends Instr {
  public static <T extends NamedElement> ElementMatcher.Junction<T> nameStartsWith(Set<String> prefix) {
    return new NameMatcher<T>(new StringSetMatcherStartsWith(prefix));
  }

  public ByteBuddyInstr(Set<String> includes) {
    super(includes);
  }

  @Override
  public void instrument(Instrumentation instrumentation) {
    final Advice methodAdvice = Advice.to(MethodTracer.class);
    final Advice testMethodAdvice = Advice.to(TestMethodTracer.class);
    final Advice constructorAdvice = Advice.to(ConstructorTracer.class);
    final Advice testConstructorAdvice = Advice.to(TestConstructorTracer.class);

    new AgentBuilder.Default()
        .disableClassFormatChanges()
        //        .with(new TracerLogger())
        .type(nameStartsWith(includes))
        .transform((builder, typeDescription, classLoader, module) -> {
          builder = builder.visit(new AsmVisitorWrapper.ForDeclaredMethods().method(
              ElementMatchers.isMethod().and(
                  ElementMatchers.isAnnotatedWith(org.junit.Test.class)
                      .or(ElementMatchers.isAnnotatedWith(Test.class))
                      .or(ElementMatchers.isAnnotatedWith(org.testng.annotations.Test.class))),
              testMethodAdvice));

          builder = builder.visit(new AsmVisitorWrapper.ForDeclaredMethods().method(
              ElementMatchers.isMethod().and(
                  ElementMatchers.not(ElementMatchers.isAnnotatedWith(org.junit.Test.class)
                                          .or(ElementMatchers.isAnnotatedWith(Test.class))
                                          .or(ElementMatchers.isAnnotatedWith(org.testng.annotations.Test.class)))),
              methodAdvice));

          builder = builder.visit(new AsmVisitorWrapper.ForDeclaredMethods().constructor(
              ElementMatchers.isConstructor().and(
                  ElementMatchers.isAnnotatedWith(org.junit.Test.class)
                      .or(ElementMatchers.isAnnotatedWith(Test.class))
                      .or(ElementMatchers.isAnnotatedWith(org.testng.annotations.Test.class))),
              testConstructorAdvice));

          builder = builder.visit(new AsmVisitorWrapper.ForDeclaredMethods().constructor(
              ElementMatchers.isConstructor().and(
                  ElementMatchers.not(ElementMatchers.isAnnotatedWith(org.junit.Test.class)
                                          .or(ElementMatchers.isAnnotatedWith(Test.class))
                                          .or(ElementMatchers.isAnnotatedWith(org.testng.annotations.Test.class)))),
              constructorAdvice));

          return builder;
        })
        .installOn(instrumentation);
  }
}