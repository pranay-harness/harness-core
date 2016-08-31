package software.wings.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import software.wings.security.PermissionAttribute.PermissionScope;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Created by anubhaw on 8/30/16.
 */
public class PermissionAttributeTest {
  @Test
  public void shouldValidateAllPermissionBits() {
    Reflections reflections = new Reflections(new ConfigurationBuilder()
                                                  .setUrls(ClasspathHelper.forPackage("software.wings"))
                                                  .setScanners(new MethodAnnotationsScanner()));
    Set<Method> methods = reflections.getMethodsAnnotatedWith(AuthRule.class);
    methods.forEach(method -> {
      String[] permissionStrings = method.getAnnotation(AuthRule.class).value();
      PermissionScope permissionScope = method.getAnnotation(AuthRule.class).scope();
      assertThat(permissionScope).isIn(PermissionAttribute.PermissionScope.values());
      Stream.of(permissionStrings).forEach(permissionString -> {
        String[] permissionAttributes = permissionString.split(":");
        assertThat(permissionAttributes.length).isEqualTo(2);
        assertThat(ResourceType.valueOf(permissionAttributes[0])).isNotNull();
        assertThat(PermissionAttribute.Action.valueOf(permissionAttributes[1])).isNotNull();
      });
    });
  }
}
