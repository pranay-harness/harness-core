package software.wings.security.annotations;

import software.wings.beans.HarnessApiKey.ClientType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface HarnessApiKeyAuth {
  ClientType[] clientTypes();
}
