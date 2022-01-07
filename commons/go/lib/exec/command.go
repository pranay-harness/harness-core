// Copyright 2020 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package exec

import (
	"fmt"
	"io"
	"os"
	"os/exec"
)

//go:generate mockgen -source command.go -destination command_mock.go -package exec CommandFactory CommandContextFactory Command

//CommandFactory describes an object capable of making a Command
type CommandFactory interface {
	Command(name string, args ...string) Command
}

type osCmd struct {
	*exec.Cmd
}

//Command is an object that can run a command. An os/exec.Cmd is one of these
type Command interface {
	Start() error
	Wait() error
	Run() error
	Output() ([]byte, error)
	CombinedOutput() ([]byte, error)
	String() string
	StdinPipe() (io.WriteCloser, error)
	StderrPipe() (io.ReadCloser, error)
	StdoutPipe() (io.ReadCloser, error)

	// Pid returns the process IF of a running process. Returns -1 if the process has not yet been started
	Pid() int

	//ProcessState returns information about the command once it has exited. Returns nil
	//if the command has not yet exited (i.e., Wait has not returned).
	// ProcessState is available after a call to Wait or Run.
	ProcessState() ProcessState

	//WithEnvVars adds the given key value pairs to the command's environment, as KEY=VALUE strings.
	// If an odd number of arguments in provided, the last one will be dropped and not added to the env
	// If empty kvps are provided, it simply uses OS env variables without overriding them
	WithEnvVars(kvps ...string) Command

	//WithEnvVarsMap adds the given key value pairs map to the command's environment, as KEY=VALUE strings.
	// If empty kvps are provided, it simply uses OS env variables without overriding them
	WithEnvVarsMap(kvps map[string]string) Command

	//WithDir sets the working directory of the command to be the given directory. If the directory
	//is the empty string, then the command will be launched in the calling processes's working directory
	WithDir(string) Command

	//WithStdout sets the Stdout of the process to be directed to the given io.Writer
	WithStdout(writer io.Writer) Command

	//WithSterr sets the Stderr of the process to be directed to the given io.Writer
	WithStderr(writer io.Writer) Command

	//WithCombinedOutput sets the Stdout and Stderr of the given command to the given io.Writer.
	//Equivalent to calling cmd.WithStdout(w).WithStderr(w)
	WithCombinedOutput(writer io.Writer) Command
}

//CommandFactoryFunc is an adapter allowing a single function to be used as a CommandFactory
type CommandFactoryFunc func(name string, args ...string) Command

//Command implements CommandFactory, calling the underlying function
func (cff CommandFactoryFunc) Command(name string, args ...string) Command {
	return cff(name, args...)
}

// osCommand is a CommandFactory that creates an os/exec.Cmd
var osCommand CommandFactory = CommandFactoryFunc(func(name string, args ...string) Command {
	return &osCmd{exec.Command(name, args...)}
})

//OsCommand returns CommandFactory that creates an os/exec.Cmd
func OsCommand() CommandFactory {
	return CommandFactoryFunc(func(name string, args ...string) Command {
		return &osCmd{exec.Command(name, args...)}
	})
}

func (o *osCmd) Pid() int {
	if o.Process == nil {
		return -1
	}
	return o.Process.Pid
}

func (o *osCmd) ProcessState() ProcessState {
	if o.Cmd.ProcessState != nil {
		return &osProcessState{o.Cmd.ProcessState}
	}
	return nil
}

func (o *osCmd) WithEnvVars(kvps ...string) Command {
	cpy := *o

	cpy.Env = make([]string, len(o.Env))
	copy(cpy.Env, o.Env)

	//Env specifies the environment of the process.
	// If Env is nil, the new process uses the current process's environment.
	// However, when the o.Env is of size 0, we end up not using default env variables. S
	// So, we are assigning it explicitly below
	if len(cpy.Env) == 0 {
		cpy.Env = os.Environ()
	}

	for i := 0; i+1 < len(kvps); i += 2 {
		key := kvps[i]
		val := kvps[i+1]
		cpy.Env = append(cpy.Env, fmt.Sprintf("%s=%s", key, val))
	}
	return &cpy
}

func (o *osCmd) WithEnvVarsMap(kvps map[string]string) Command {
	cpy := *o

	cpy.Env = make([]string, len(o.Env))
	copy(cpy.Env, o.Env)
	//Env specifies the environment of the process.
	// If Env is nil, the new process uses the current process's environment.
	// However, when the o.Env is of size 0, we end up not using default env variables. S
	// So, we are assigning it explicitly below
	if len(cpy.Env) == 0 {
		cpy.Env = os.Environ()
	}

	for key, val := range kvps {
		cpy.Env = append(cpy.Env, fmt.Sprintf("%s=%s", key, val))
	}
	return &cpy
}

func (o *osCmd) WithDir(directory string) Command {
	cpy := *o
	cpy.Dir = directory
	return &cpy
}

func (o *osCmd) WithStdout(writer io.Writer) Command {
	cpy := *o
	cpy.Stdout = writer
	return &cpy
}

func (o *osCmd) WithStderr(writer io.Writer) Command {
	cpy := *o
	cpy.Stderr = writer
	return &cpy
}

func (o *osCmd) WithCombinedOutput(writer io.Writer) Command {
	cpy := *o
	cpy.Stdout = writer
	cpy.Stderr = writer
	return &cpy
}
