package software.wings.graphql.utils.nameservice;

import java.util.Set;
import javax.validation.constraints.NotNull;

public interface NameService { NameResult getNames(@NotNull Set<String> ids, @NotNull String type); }
