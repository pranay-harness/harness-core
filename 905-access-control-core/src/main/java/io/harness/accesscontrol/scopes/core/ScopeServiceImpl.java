package io.harness.accesscontrol.scopes.core;

import static io.harness.accesscontrol.scopes.core.Scope.PATH_DELIMITER;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
public class ScopeServiceImpl implements ScopeService {
  private final Map<Integer, ScopeLevel> scopeLevelsByRank;
  private final Map<String, ScopeLevel> scopeLevelsByResourceType;
  private final Map<String, ScopeLevel> scopeLevels;
  private final int lowestLevel;

  @Inject
  public ScopeServiceImpl(Map<String, ScopeLevel> scopeLevels) {
    this.scopeLevels = scopeLevels;
    this.scopeLevelsByRank = new HashMap<>();
    this.scopeLevelsByResourceType = new HashMap<>();
    scopeLevels.values().forEach(scopeLevel -> scopeLevelsByRank.put(scopeLevel.getRank(), scopeLevel));
    scopeLevels.values().forEach(scopeLevel -> scopeLevelsByResourceType.put(scopeLevel.getResourceType(), scopeLevel));
    if (scopeLevelsByRank.get(0) == null) {
      throw new InvalidArgumentsException("No root scope level has been registered");
    }
    lowestLevel = scopeLevelsByRank.keySet().stream().max(Comparator.naturalOrder()).orElse(0);
  }

  @Override
  public Scope buildScopeFromParams(ScopeParams scopeParams) {
    Map<String, String> params = scopeParams.getParams();
    ScopeLevel rootScopeLevel = scopeLevelsByRank.get(0);
    String rootInstanceId = params.get(rootScopeLevel.getParamName());
    if (isEmpty(rootInstanceId)) {
      throw new InvalidRequestException(
          String.format("The %s is not provided in the scope parameters", rootScopeLevel.getParamName()));
    }
    Scope scope = Scope.builder().level(rootScopeLevel).instanceId(rootInstanceId).parentScope(null).build();
    for (int currentLevel = 1; currentLevel <= lowestLevel; currentLevel++) {
      ScopeLevel scopeLevel = scopeLevelsByRank.get(currentLevel);
      if (scopeLevel != null) {
        String instanceId = params.get(scopeLevel.getParamName());
        if (isNotEmpty(instanceId)) {
          scope = Scope.builder().level(scopeLevel).instanceId(instanceId).parentScope(scope).build();
        }
      }
    }
    return scope;
  }

  @Override
  public Scope buildScopeFromScopeIdentifier(String scopeIdentifier) {
    List<String> scopeIdentifierElements = Arrays.asList(scopeIdentifier.split(PATH_DELIMITER));
    if (scopeIdentifierElements.size() < 3) {
      return null;
    }
    String instanceId = scopeIdentifierElements.get(scopeIdentifierElements.size() - 1);
    ScopeLevel scopeLevel =
        scopeLevelsByResourceType.get(scopeIdentifierElements.get(scopeIdentifierElements.size() - 2));
    if (isEmpty(instanceId)) {
      throw new InvalidArgumentsException("The instance id is invalid or empty in the scopeIdentifier");
    }
    if (scopeLevel == null) {
      throw new InvalidRequestException(
          "The scope level mentioned in the scope identifier is not registered in the system");
    }
    String parentScopeIdentifier =
        String.join(PATH_DELIMITER, scopeIdentifierElements.subList(0, scopeIdentifierElements.size() - 2));
    return Scope.builder()
        .instanceId(instanceId)
        .level(scopeLevel)
        .parentScope(buildScopeFromScopeIdentifier(parentScopeIdentifier))
        .build();
  }

  @Override
  public boolean areScopeLevelsValid(Set<String> scopeLevelsToValidate) {
    return scopeLevelsToValidate.stream().allMatch(scopeLevel -> scopeLevels.get(scopeLevel) != null);
  }

  @Override
  public Set<String> getAllScopeLevels() {
    return scopeLevels.keySet();
  }
}
