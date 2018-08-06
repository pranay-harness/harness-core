package io.harness;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.lang.reflect.Field;

@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public class MockableTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  public static void setStaticFieldValue(final Class<?> clz, final String fieldName, final Object value)
      throws IllegalAccessException {
    final Field f = FieldUtils.getField(clz, fieldName, true);
    FieldUtils.removeFinalModifier(f);
    f.set(null, value);
  }
}
