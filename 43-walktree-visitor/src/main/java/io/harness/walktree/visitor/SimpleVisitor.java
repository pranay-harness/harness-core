package io.harness.walktree.visitor;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;

import io.harness.walktree.beans.VisitElementResult;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public abstract class SimpleVisitor<T> extends Visitor {
  private final Injector injector;
  Map<String, Object> contextMap = new ConcurrentHashMap<>();

  @Inject
  public SimpleVisitor(Injector injector) {
    this.injector = injector;
  }

  @Override
  public VisitElementResult preVisitElement(Object element) {
    Objects.requireNonNull(element);
    return VisitElementResult.CONTINUE;
  }

  @Override
  public VisitElementResult postVisitElement(Object element) {
    Objects.requireNonNull(element);
    return VisitElementResult.CONTINUE;
  }

  public T getHelperClass(Object currentElement) {
    if (currentElement.getClass().isAnnotationPresent(SimpleVisitorHelper.class)) {
      Class<?> helperClass = currentElement.getClass().getAnnotation(SimpleVisitorHelper.class).helperClass();
      return (T) injector.getInstance(Key.get(helperClass));
    }
    return null;
  }

  public <A> Optional<A> getAttribute(String key) {
    return Optional.ofNullable((A) contextMap.get(key));
  }

  public <A> void addAttribute(String key, A value) {
    contextMap.put(key, value);
  }

  public void removeAttribute(String key) {
    contextMap.remove(key);
  }
}
