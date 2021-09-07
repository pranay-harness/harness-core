package io.harness.cvng.core.beans.params.filterParams;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
public abstract class LogAnalysisFilter extends AnalysisFilter {}
