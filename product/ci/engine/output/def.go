// Package output provides the definitions for stage and step outputs
package output

// StageOutput is combination of outputs produced by steps in a stage. It can be referenced
// by a step using expression values e.g. JEXL. At start of a stage, stage output is empty.
//
// StageOutput is the format for output/outcomes for a stage
// Key in the map is step ID.
type StageOutput map[string]*StepOutput

// StepOutput is the format for output/outcomes for a step
type StepOutput struct {
	Output struct {
		Variables map[string]string `json:"outputVariables"`
	} `json:"output"`
}
