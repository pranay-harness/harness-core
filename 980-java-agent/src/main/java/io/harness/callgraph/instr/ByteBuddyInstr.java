package io.harness.callgraph.instr;

import io.harness.callgraph.instr.tracer.ConstructorTracer;
import io.harness.callgraph.instr.tracer.MethodTracer;
import io.harness.callgraph.instr.tracer.TestConstructorTracer;
import io.harness.callgraph.instr.tracer.TestMethodTracer;

import java.lang.instrument.Instrumentation;
import java.util.List;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.ResettableClassFileTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import org.junit.jupiter.api.Test;

/**
 * Instrument the target classes
 */
public class ByteBuddyInstr extends Instr {
  public static ElementMatcher.Junction nameStartsWithAnyOf(List<String> includes) {
    ElementMatcher.Junction matcher = null;
    for (String pkg : includes) {
      matcher = matcher == null ? ElementMatchers.nameStartsWith(pkg) : matcher.or(ElementMatchers.nameStartsWith(pkg));
    }
    return matcher == null ? ElementMatchers.none() : matcher;
  }
  public ByteBuddyInstr(List<String> includes) {
    super(includes);
  }

  @Override
  public void instrument(Instrumentation instrumentation) {
    final Advice methodAdvice = Advice.to(MethodTracer.class);
    final Advice testMethodAdvice = Advice.to(TestMethodTracer.class);
    final Advice constructorAdvice = Advice.to(ConstructorTracer.class);
    final Advice testConstructorAdvice = Advice.to(TestConstructorTracer.class);

    ElementMatcher.Junction matcher = nameStartsWithAnyOf(includes);

    ResettableClassFileTransformer agent =
        new AgentBuilder.Default()
            .with(new TracerLogger())
            .type(matcher)
            .transform((builder, typeDescription, classLoader, module) -> {
              builder = builder.visit(new AsmVisitorWrapper.ForDeclaredMethods().method(
                  ElementMatchers.isMethod().and(ElementMatchers.isAnnotatedWith(org.junit.Test.class)
                                                     .or(ElementMatchers.isAnnotatedWith(Test.class))),
                  testMethodAdvice));
              builder = builder.visit(new AsmVisitorWrapper.ForDeclaredMethods().method(
                  ElementMatchers.isMethod().and(
                      ElementMatchers.not(ElementMatchers.isAnnotatedWith(org.junit.Test.class)
                                              .or(ElementMatchers.isAnnotatedWith(Test.class)))),
                  methodAdvice));
              builder = builder.visit(new AsmVisitorWrapper.ForDeclaredMethods().constructor(
                  ElementMatchers.isConstructor().and(ElementMatchers.isAnnotatedWith(org.junit.Test.class)
                                                          .or(ElementMatchers.isAnnotatedWith(Test.class))),
                  testConstructorAdvice));
              builder = builder.visit(new AsmVisitorWrapper.ForDeclaredMethods().constructor(
                  ElementMatchers.isConstructor().and(
                      ElementMatchers.not(ElementMatchers.isAnnotatedWith(org.junit.Test.class)
                                              .or(ElementMatchers.isAnnotatedWith(Test.class)))),
                  constructorAdvice));

              return builder;
            })
            .installOn(instrumentation);
  }
}