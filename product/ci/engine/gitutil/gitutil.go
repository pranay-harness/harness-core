package gitutil

import (
	"fmt"
	"os/exec"
	"strings"
)

const (
	command = "%s diff --name-only HEAD HEAD@{1} -1"
)

// GetChangedFiles executes a shell command and retuns list of files changed in PR
func GetChangedFiles(gitPath, workspace string) ([]string, error) {
	cmd := exec.Command("sh", "-c", fmt.Sprintf(command, gitPath))
	cmd.Dir = workspace
	out, err := cmd.Output()

	if err != nil {
		return nil, err
	}
	return strings.Split(string(out), "\n"), nil
}
