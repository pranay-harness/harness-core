package io.harness.cf.openapi.api;

public class PatchInstruction {
  private String kind;
  private Object parameters;

  public static final class PatchInstructionBuilder {
    private String kind;
    private Object parameters;

    private PatchInstructionBuilder() {}

    public static PatchInstructionBuilder aPatchInstruction() {
      return new PatchInstructionBuilder();
    }

    public PatchInstructionBuilder withKind(String kind) {
      this.kind = kind;
      return this;
    }

    public PatchInstructionBuilder withParameters(Object parameters) {
      this.parameters = parameters;
      return this;
    }

    public PatchInstruction build() {
      PatchInstruction patchInstruction = new PatchInstruction();
      patchInstruction.parameters = this.parameters;
      patchInstruction.kind = this.kind;
      return patchInstruction;
    }
  }
}
