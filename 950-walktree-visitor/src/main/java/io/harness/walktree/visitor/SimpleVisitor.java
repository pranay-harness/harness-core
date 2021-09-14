/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.walktree.visitor;

import io.harness.exception.InvalidArgumentsException;
import io.harness.walktree.beans.VisitElementResult;

import com.google.inject.Injector;
import com.google.inject.Key;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class SimpleVisitor<T extends DummyVisitableElement> extends Visitor {
  private Injector injector;
  @Getter Map<String, Object> contextMap = new ConcurrentHashMap<>();
  @Getter Map<Object, Object> elementToDummyElementMap = new HashMap<>();

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

  /**
   * This function returns new dummy object for the currentElement.
   * @param currentElement
   * @return Dummy Object
   */
  protected Object getNewDummyObject(Object currentElement) {
    DummyVisitableElement helperClass = getHelperClass(currentElement);
    Object dummyElement;
    if (helperClass != null) {
      dummyElement = helperClass.createDummyVisitableElement(currentElement);
    } else {
      log.error("Helper Class not implemented for object of type" + currentElement.getClass());
      throw new InvalidArgumentsException(
          "Helper Class not implemented for object of type" + currentElement.getClass());
    }
    return dummyElement;
  }
}
