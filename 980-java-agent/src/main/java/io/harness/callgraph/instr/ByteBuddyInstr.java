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

/**
 * Instrument the target classes
 */
public class ByteBuddyInstr extends Instr {
  public static <T extends NamedElement> ElementMatcher.Junction<T> nameStartsWith(Set<String> prefix) {
    return new NameMatcher<T>(new StringSetMatcherStartsWith(prefix));
  }

  public ByteBuddyInstr(Set<String> includes, Set<String> testAnnotations) {
    super(includes, testAnnotations);
  }

  @Override
  public void instrument(Instrumentation instrumentation) {
    final Advice methodAdvice = Advice.to(MethodTracer.class).withExceptionPrinting();
    final Advice testMethodAdvice = Advice.to(TestMethodTracer.class).withExceptionPrinting();
    final Advice constructorAdvice = Advice.to(ConstructorTracer.class).withExceptionPrinting();
    final Advice testConstructorAdvice = Advice.to(TestConstructorTracer.class).withExceptionPrinting();

    new AgentBuilder.Default()
        .disableClassFormatChanges()
        //        .with(new TracerLogger())
        .type(nameStartsWith(includes))
        .transform((builder, typeDescription, classLoader, module) -> {
          builder = builder.visit(new AsmVisitorWrapper.ForDeclaredMethods().method(
              ElementMatchers.isMethod().and(

                  ElementMatchers.declaresAnnotation(
                      annotation -> testAnnotations.contains(annotation.getAnnotationType().getName()))),
              testMethodAdvice));

          builder = builder.visit(new AsmVisitorWrapper.ForDeclaredMethods().constructor(
              ElementMatchers.isConstructor().and(ElementMatchers.declaresAnnotation(
                  annotation -> testAnnotations.contains(annotation.getAnnotationType().getName()))),
              testConstructorAdvice));

          builder = builder.visit(new AsmVisitorWrapper.ForDeclaredMethods().method(
              ElementMatchers.isMethod().and(ElementMatchers.not(

                  ElementMatchers.declaresAnnotation(
                      annotation -> testAnnotations.contains(annotation.getAnnotationType().getName())))),
              methodAdvice));

          builder = builder.visit(new AsmVisitorWrapper.ForDeclaredMethods().constructor(
              ElementMatchers.isConstructor().and(ElementMatchers.not(

                  ElementMatchers.declaresAnnotation(
                      annotation -> testAnnotations.contains(annotation.getAnnotationType().getName())))),
              constructorAdvice));
          return builder;
        })
        .installOn(instrumentation);
  }
}