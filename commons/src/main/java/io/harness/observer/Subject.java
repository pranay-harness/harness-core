package io.harness.observer;

import static java.util.stream.Collectors.toList;

import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class Subject<T> {
  private List<T> observers = new ArrayList<>();

  public void register(@NonNull T observer) {
    observers.add(observer);
  }

  public void unregister(@NonNull T observer) {
    observers.remove(observer);
  }

  public <U> List<Rejection> fireApproveFromAll(BiFunction<T, U, Rejection> func, U arg) {
    return observers.stream()
        .map(observer -> func.apply(observer, arg))
        .filter(rejection -> rejection != null)
        .collect(toList());
  }
}
