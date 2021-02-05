package io.harness.accesscontrol.scopes;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;

import io.harness.exception.InvalidArgumentsException;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
public class ScopeServiceImpl implements ScopeService {
  private final Map<String, Scope> scopesByIdentifierKey;
  private final Map<String, Scope> scopesByPathKey;

  @Inject
  public ScopeServiceImpl(@Named(SCOPES_BY_IDENTIFIER_KEY) Map<String, Scope> scopesByIdentifierKey,
      @Named(SCOPES_BY_PATH_KEY) Map<String, Scope> scopesByPathKey) {
    this.scopesByIdentifierKey = scopesByIdentifierKey;
    this.scopesByPathKey = scopesByPathKey;
  }

  @Override
  public Scope getScope(Map<String, String> scopeIdentifiers) {
    return null;
  }

  @Override
  public String getScopeIdentifier(@NotNull Map<String, String> scopeInstancesByIdentifierKey) {
    List<String> validIdentifierKeys = scopeInstancesByIdentifierKey.keySet()
                                           .stream()
                                           .filter(key -> isNotEmpty(scopeInstancesByIdentifierKey.get(key)))
                                           .collect(Collectors.toList());
    validIdentifierKeys.forEach(key -> {
      if (scopesByIdentifierKey.get(key) == null) {
        throw new InvalidArgumentsException(
            "Invalid scope. The given identifier is either invalid or not registered with the service", USER);
      }
    });
    List<String> subPaths = validIdentifierKeys.stream()
                                .map(scopesByIdentifierKey::get)
                                .sorted(Comparator.comparingInt(Scope::getRank))
                                .map(scope
                                    -> "/".concat(scope.getPathKey())
                                           .concat("/")
                                           .concat(scopeInstancesByIdentifierKey.get(scope.getIdentifierKey())))
                                .collect(Collectors.toList());

    return String.join("", subPaths);
  }

  @Override
  public Map<String, String> getIdentifiers(String scopeIdentifier) {
    Map<String, String> scopeInstancesByPathKey =
        Arrays.stream(scopeIdentifier.split("/")).collect(IdentifierCollector.collector());
    Map<String, String> scopeInstancesByIdentifierKey = new HashMap<>();
    scopeInstancesByPathKey.forEach((pathKey, scopeInstance) -> {
      Scope scope = scopesByPathKey.get(pathKey);
      if (scope == null) {
        throw new InvalidArgumentsException(
            "Invalid scope. The given path is either invalid or not registered with the service", USER);
      }
      scopeInstancesByIdentifierKey.put(scope.getIdentifierKey(), scopeInstance);
    });
    return scopeInstancesByIdentifierKey;
  }

  public static final class IdentifierCollector {
    private final Map<String, String> map = new HashMap<>();

    private String key;

    public void accept(String str) {
      if (isEmpty(key)) {
        key = str;
      } else {
        map.put(new String(key), str);
        key = null;
      }
    }

    public IdentifierCollector combine(IdentifierCollector other) {
      throw new UnsupportedOperationException("Parallel Stream not supported");
    }

    public Map<String, String> finish() {
      return map;
    }

    public static Collector<String, ?, Map<String, String>> collector() {
      return Collector.of(IdentifierCollector::new, IdentifierCollector::accept, IdentifierCollector::combine,
          IdentifierCollector::finish);
    }
  }
}
