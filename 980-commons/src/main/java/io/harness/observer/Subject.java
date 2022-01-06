/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.observer;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.NonNull;

public class Subject<T> {
  @FunctionalInterface
  public interface Approver<T, U, R> {
    R apply(T t, U u);
  }

  @FunctionalInterface
  public interface Informant0<T> {
    void inform(T t);
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

  @FunctionalInterface
  public interface Processor0<T, U> {
    U process(T t, U u);
  }

  @FunctionalInterface
  public interface Processor1<T, U1, U2> {
    U2 process(T t, U1 u1, U2 u2);
  }

  private List<T> observers = new CopyOnWriteArrayList<>();

  public void register(@NonNull T observer) {
    observers.add(observer);
  }

  public void unregister(@NonNull T observer) {
    observers.remove(observer);
  }

  public <U> List<Rejection> fireApproveFromAll(Approver<T, U, Rejection> func, U arg) {
    return observers.stream().map(observer -> func.apply(observer, arg)).filter(Objects::nonNull).collect(toList());
  }

  public void fireInform(Informant0<T> func) {
    observers.forEach(observer -> {
      if (observer instanceof AsyncInformObserver) {
        ((AsyncInformObserver) observer).getInformExecutorService().submit(() -> func.inform(observer));
      } else {
        func.inform(observer);
      }
    });
  }

  public <U> void fireInform(Informant1<T, U> func, U arg) {
    observers.forEach(observer -> {
      if (observer instanceof AsyncInformObserver) {
        ((AsyncInformObserver) observer).getInformExecutorService().submit(() -> func.inform(observer, arg));
      } else {
        func.inform(observer, arg);
      }
    });
  }

  public <U1, U2> void fireInform(Informant2<T, U1, U2> func, U1 arg1, U2 arg2) {
    observers.forEach(observer -> {
      if (observer instanceof AsyncInformObserver) {
        ((AsyncInformObserver) observer).getInformExecutorService().submit(() -> func.inform(observer, arg1, arg2));
      } else {
        func.inform(observer, arg1, arg2);
      }
    });
  }

  public <U1, U2, U3> void fireInform(Informant3<T, U1, U2, U3> func, U1 arg1, U2 arg2, U3 arg3) {
    observers.forEach(observer -> {
      if (observer instanceof AsyncInformObserver) {
        ((AsyncInformObserver) observer)
            .getInformExecutorService()
            .submit(() -> func.inform(observer, arg1, arg2, arg3));
      } else {
        func.inform(observer, arg1, arg2, arg3);
      }
    });
  }

  public <U1, U2, U3, U4> void fireInform(Informant4<T, U1, U2, U3, U4> func, U1 arg1, U2 arg2, U3 arg3, U4 arg4) {
    observers.forEach(observer -> {
      if (observer instanceof AsyncInformObserver) {
        ((AsyncInformObserver) observer)
            .getInformExecutorService()
            .submit(() -> func.inform(observer, arg1, arg2, arg3, arg4));
      } else {
        func.inform(observer, arg1, arg2, arg3, arg4);
      }
    });
  }

  public <U> U fireProcess(Processor0<T, U> func, U data) {
    for (T observer : observers) {
      data = func.process(observer, data);
    }
    return data;
  }

  public <U1, U2> U2 fireProcess(Processor1<T, U1, U2> func, U1 id, U2 data) {
    for (T observer : observers) {
      data = func.process(observer, id, data);
    }
    return data;
  }
}
