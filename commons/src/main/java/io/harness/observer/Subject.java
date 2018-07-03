package io.harness.observer;

import static java.util.stream.Collectors.toList;

import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;

public class Subject<T> {
  @FunctionalInterface
  public interface Approver<T, U, R> {
    R apply(T t, U u);
  }

  @FunctionalInterface
  public interface Informant1<T, U> {
    void inform(T t, U u);
  }

  @FunctionalInterface
  public interface Informant2<T, U1, U2> {
    void inform(T t, U1 u1, U2 u2);
  }

  @FunctionalInterface
  public interface Informant3<T, U1, U2, U3> {
    void inform(T t, U1 u1, U2 u2, U3 u3);
  }

  @FunctionalInterface
  public interface Informant4<T, U1, U2, U3, U4> {
    void inform(T t, U1 u1, U2 u2, U3 u3, U4 u4);
  }

  private List<T> observers = new ArrayList<>();

  public void register(@NonNull T observer) {
    observers.add(observer);
  }

  public void unregister(@NonNull T observer) {
    observers.remove(observer);
  }

  public <U> List<Rejection> fireApproveFromAll(Approver<T, U, Rejection> func, U arg) {
    return observers.stream()
        .map(observer -> func.apply(observer, arg))
        .filter(rejection -> rejection != null)
        .collect(toList());
  }

  public <U> void fireInform(Informant1<T, U> func, U arg) {
    observers.forEach(observer -> func.inform(observer, arg));
  }

  public <U1, U2> void fireInform(Informant2<T, U1, U2> func, U1 arg1, U2 arg2) {
    observers.forEach(observer -> func.inform(observer, arg1, arg2));
  }

  public <U1, U2, U3> void fireInform(Informant3<T, U1, U2, U3> func, U1 arg1, U2 arg2, U3 arg3) {
    observers.forEach(observer -> func.inform(observer, arg1, arg2, arg3));
  }

  public <U1, U2, U3, U4> void fireInform(Informant4<T, U1, U2, U3, U4> func, U1 arg1, U2 arg2, U3 arg3, U4 arg4) {
    observers.forEach(observer -> func.inform(observer, arg1, arg2, arg3, arg4));
  }
}
