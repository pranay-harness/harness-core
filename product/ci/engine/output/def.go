// Package output provides the definitions for stage and step outputs
package output

// StageOutput is the format for output/outcomes for a stage
// Key in the map is step ID.
type StageOutput map[string]*StepOutput

// StepOutput is the format for output/outcomes for a step
type StepOutput struct {
	Output map[string]string `json:"output"`
}
