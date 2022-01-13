// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package exec

import (
	"bytes"
	"context"
	"fmt"
	"io"
	"os"
	"os/exec"
	"time"

	"github.com/pkg/errors"
	"go.uber.org/zap"
)

const defSleep = 5

//go:generate mockgen -source command_context.go -package=exec -destination command_context_mock.go CmdContextFactory

//CmdContextFactory describes an object capable of making a Command with a context
type CmdContextFactory interface {
	CmdContext(ctx context.Context, name string, args ...string) Command
	CmdContextWithSleep(ctx context.Context, sleep time.Duration, name string, args ...string) Command
}

type osCmdContext struct {
	ctx  context.Context
	wait chan struct{}
	log  *zap.SugaredLogger
	*exec.Cmd
	sleep time.Duration
}

//CmdContextFactoryFunc is an adapter allowing a single function to be used as a CmdContextFactory
type CmdContextFactoryFunc func(ctx context.Context, sleep time.Duration, name string, args ...string) Command

//CmdContext implements CmdContextFactory, calling the underlying function
func (cff CmdContextFactoryFunc) CmdContext(ctx context.Context, name string, args ...string) Command {
	return cff(ctx, defSleep*time.Second, name, args...)
}

//CmdContext implements CmdContextFactory, calling the underlying function. It uses the provided sleep duration
func (cff CmdContextFactoryFunc) CmdContextWithSleep(ctx context.Context, sleep time.Duration, name string, args ...string) Command {
	return cff(ctx, sleep, name, args...)
}

// osCommandContext is a CmdContextFactory that creates an os/exec.Cmd with a context (calls os/exec.CmdContext()).
//When the context exits, the process is killed and the command does not wait for it to exit before returning from Wait().
var osCommandContext CmdContextFactory = CmdContextFactoryFunc(func(ctx context.Context, sleep time.Duration, name string, args ...string) Command {
	return &osCmd{exec.CommandContext(ctx, name, args...)}
})

//osCommandContextGraceful is like osCommandContext, but instead of sending the process a
//kill when the context completes before the process does, it sends a SIGTERM, waits 5 seconds
//and then sends a SIGKILL
var osCommandContextGraceful = OsCommandContextGracefulWithLog(zap.NewNop().Sugar())

//OsCommandContextGracefulWithLog logs before sending
func OsCommandContextGracefulWithLog(log *zap.SugaredLogger) CmdContextFactory {
	return CmdContextFactoryFunc(func(ctx context.Context, sleep time.Duration, name string, args ...string) Command {
		return &osCmdContext{
			ctx:   ctx,
			log:   log,
			Cmd:   exec.Command(name, args...),
			sleep: sleep,
		}
	})
}

func (o *osCmdContext) Start() error {
	err := o.Cmd.Start()
	if err != nil {
		return err
	}

	// o.log.Debugw("cmd started", "cmd", o.String(), "pid", o.Pid())
	o.wait = make(chan struct{})
	go func() {
		select {
		case <-o.ctx.Done():
			// The only signal values guaranteed to be present in the os package on all systems are
			//os.Interrupt(send the process an interrupt) and os.Kill (force the process to exit).
			//https://golang.org/pkg/os/#Signal
			//o.log.Infow("signaling process", "signal", os.Interrupt.String(), "cmd", o.String(), "pid", o.Pid())
			o.Process.Signal(os.Interrupt)

			time.Sleep(o.sleep)
			//o.log.Infow("signaling process", "signal", os.Kill.String(), "cmd", o.String(), "pid", o.Pid())
			o.Process.Signal(os.Kill)

		case <-o.wait:
			// o.log.Debugw("cmd completed", "cmd", o.String(), "pid", o.Pid())
		}
	}()
	return err
}

func (o *osCmdContext) Wait() error {
	if o.wait == nil {
		return errors.New("cmd has not been started yet")
	}
	defer close(o.wait)

	return o.Cmd.Wait()
}

func (o *osCmdContext) Run() error {
	err := o.Start()
	if err != nil {
		return err
	}
	return o.Wait()
}

func (o *osCmdContext) Output() ([]byte, error) {
	buffer := bytes.NewBuffer(nil)

	o.Cmd.Stdout = buffer

	err := o.Run()
	return buffer.Bytes(), err
}

func (o *osCmdContext) CombinedOutput() ([]byte, error) {
	buffer := bytes.NewBuffer(nil)

	o.Cmd.Stdout = buffer
	o.Cmd.Stderr = buffer

	err := o.Run()
	return buffer.Bytes(), err
}

func (o *osCmdContext) Pid() int {
	if o.Process == nil {
		return -1
	}
	return o.Process.Pid
}

func (o *osCmdContext) ProcessState() ProcessState {
	if o.Cmd.ProcessState != nil {
		return &osProcessState{o.Cmd.ProcessState}
	}
	return nil
}

func (o *osCmdContext) WithEnvVars(kvps ...string) Command {
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

func (o *osCmdContext) WithEnvVarsMap(kvps map[string]string) Command {
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

func (o *osCmdContext) WithDir(directory string) Command {
	cpy := *o
	cpy.Dir = directory
	return &cpy
}

func (o *osCmdContext) WithStdout(writer io.Writer) Command {
	cpy := *o
	cpy.Stdout = writer
	return &cpy
}

func (o *osCmdContext) WithStderr(writer io.Writer) Command {
	cpy := *o
	cpy.Stderr = writer
	return &cpy
}

func (o *osCmdContext) WithCombinedOutput(writer io.Writer) Command {
	cpy := *o
	cpy.Stdout = writer
	cpy.Stderr = writer
	return &cpy
}
