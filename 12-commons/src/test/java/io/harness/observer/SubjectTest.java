package io.harness.observer;

import static org.junit.Assert.assertEquals;

import io.harness.category.element.UnitTests;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SubjectTest {
  private static final String OBSERVER_KEY = "observer";
  private static final String OBSERVER_1_KEY = "observer1";
  private Subject<String> subject;

  private Subject.Approver<String, String, Rejection> testFunc = (s, t) -> s.equals(t) ? null : (Rejection) () -> s;

  @Before
  public void initialize() {
    subject = new Subject<>();
  }

  @Test(expected = NullPointerException.class)
  @Category(UnitTests.class)
  public void testRegisterNPE() {
    subject.register(null);
  }

  @Test(expected = NullPointerException.class)
  @Category(UnitTests.class)
  public void testUnregisterNPE() {
    subject.unregister(null);
  }

  @Test
  @Category(UnitTests.class)
  public void testRegisterUnregister() {
    subject.register(OBSERVER_KEY);
    subject.register(OBSERVER_1_KEY);

    assertEquals(subject.fireApproveFromAll(testFunc, OBSERVER_1_KEY).size(), 1);

    subject.unregister(OBSERVER_1_KEY);
    assertEquals(subject.fireApproveFromAll(testFunc, OBSERVER_1_KEY).size(), 1);

    subject.unregister(OBSERVER_KEY);
    assertEquals(subject.fireApproveFromAll(testFunc, OBSERVER_1_KEY).size(), 0);
  }

  @Test
  @Category(UnitTests.class)
  public void testFireApproveFromAllWithArg() {
    subject.register(OBSERVER_KEY);

    assertEquals(subject.fireApproveFromAll(testFunc, OBSERVER_KEY).size(), 0);
    assertEquals(subject.fireApproveFromAll(testFunc, OBSERVER_1_KEY).size(), 1);

    subject.register(OBSERVER_1_KEY);
    assertEquals(subject.fireApproveFromAll(testFunc, OBSERVER_1_KEY).size(), 1);
  }
}
