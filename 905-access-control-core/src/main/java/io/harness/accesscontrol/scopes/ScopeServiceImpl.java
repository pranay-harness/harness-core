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
  private final Map<String, Scope> scopesByIdentifierName;
  private final Map<String, Scope> scopesByKey;

  @Inject
  public ScopeServiceImpl(@Named(SCOPES_BY_IDENTIFIER_NAME) Map<String, Scope> scopesByIdentifierName,
      @Named(SCOPES_BY_KEY) Map<String, Scope> scopesByKey) {
    this.scopesByIdentifierName = scopesByIdentifierName;
    this.scopesByKey = scopesByKey;
  }

  private List<Scope> getValidScopes(Map<String, String> scopeInstancesByIdentifierName) {
    List<String> validIdentifierKeys = scopeInstancesByIdentifierName.keySet()
                                           .stream()
                                           .filter(key -> isNotEmpty(scopeInstancesByIdentifierName.get(key)))
                                           .collect(Collectors.toList());
    validIdentifierKeys.forEach(key -> {
      if (scopesByIdentifierName.get(key) == null) {
        throw new InvalidArgumentsException(
            "Invalid scope. The given identifier is either invalid or not registered with the service", USER);
      }
    });
    return validIdentifierKeys.stream()
        .map(scopesByIdentifierName::get)
        .sorted(Comparator.comparingInt(Scope::getRank))
        .collect(Collectors.toList());
  }

  @Override
  public Map<String, Scope> getAllScopesByKey() {
    return scopesByKey;
  }

  @Override
  public Scope getLowestScope(Map<String, String> scopeInstancesByIdentifierName) {
    List<Scope> validScopes = getValidScopes(scopeInstancesByIdentifierName);
    return validScopes.get(validScopes.size() - 1);
  }

  @Override
  public String getFullyQualifiedPath(@NotNull Map<String, String> scopeInstancesByIdentifierName) {
    List<String> subPaths = getValidScopes(scopeInstancesByIdentifierName)
                                .stream()
                                .map(scope
                                    -> "/".concat(scope.getKey())
                                           .concat("/")
                                           .concat(scopeInstancesByIdentifierName.get(scope.getIdentifierName())))
                                .collect(Collectors.toList());
    return String.join("", subPaths);
  }

  @Override
  public Map<String, String> getIdentifiers(String scopeIdentifier) {
    Map<String, String> scopeInstancesByKey =
        Arrays.stream(scopeIdentifier.split("/")).collect(IdentifierCollector.collector());
    Map<String, String> scopeInstancesByIdentifierKey = new HashMap<>();
    scopeInstancesByKey.forEach((pathKey, scopeInstance) -> {
      Scope scope = scopesByKey.get(pathKey);
      if (scope == null) {
        throw new InvalidArgumentsException(
            "Invalid scope. The given path is either invalid or not registered with the service", USER);
      }
      scopeInstancesByIdentifierKey.put(scope.getIdentifierName(), scopeInstance);
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
