package io.harness.serializer.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import java.beans.Transient;

public class JsonAnnotationIntrospector extends JacksonAnnotationIntrospector {
  @Override
  protected boolean _isIgnorable(Annotated a) {
    JsonOrchestrationIgnore ann = _findAnnotation(a, JsonOrchestrationIgnore.class);
    if (ann != null) {
      return ann.value();
    }

    Transient t = a.getAnnotation(Transient.class);
    if (t != null) {
      return t.value();
    }
    return false;
  }
}
