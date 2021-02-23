package gitutil

import (
	"fmt"
	"os/exec"
	"strings"
)

const (
	command = "%s diff --name-only HEAD HEAD@{1} -1"
)

// GetChangedFiles does something
func GetChangedFiles(gitPath, workspace string) ([]string, error) {
	cmd := exec.Command("sh", "-c", fmt.Sprintf(command, gitPath))
	// cmd.Dir = "/Users/amansingh/code/portal"
	cmd.Dir = workspace
	out, err := cmd.Output()

	if err != nil {
		return nil, err
	}
	return strings.Split(string(out), "\n"), nil
}
