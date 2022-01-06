// Copyright 2020 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package exec

import (
	"os"
	"syscall"
	"time"

	"github.com/pkg/errors"
	"github.com/wings-software/portal/commons/go/lib/logs"
)

//go:generate mockgen -source process_state.go -destination process_state_mock.go -package exec ProcessState

var _ ProcessState = &osProcessState{}

//ProcessState is an interface for interacting with the current state of a process at a fairly low level
type ProcessState interface {
	ExitCode() int
	Exited() bool
	Pid() int
	String() string
	Success() bool
	SystemTime() time.Duration
	UserTime() time.Duration

	// SysUnix calls Sys and attempts to convert the result into syscall.WaitStatus
	// If not on a Unix system, always returns an error
	SysUnix() (syscall.WaitStatus, error)

	//SysUsageUnit calls SysUsage and returns the processe's rusage.
	//If not on Unix, always returns an error
	SysUsageUnit() (*syscall.Rusage, error)
}

type osProcessState struct {
	*os.ProcessState
}

func (o *osProcessState) SysUnix() (syscall.WaitStatus, error) {
	status, ok := o.Sys().(syscall.WaitStatus)
	if !ok {
		return syscall.WaitStatus(0), errors.New("unable to get unit wait status")
	}
	return status, nil
}

func (o *osProcessState) SysUsageUnit() (*syscall.Rusage, error) {
	rusage, ok := o.SysUsage().(*syscall.Rusage)
	if !ok {
		return nil, errors.New("unable to get unix rusage")
	}
	return rusage, nil
}

//ProcessStateLogFields returns interesting log fields for the given process state
//if it is not nil
func ProcessStateLogFields(ps ProcessState) (fields logs.Fields) {
	if ps == nil {
		return fields
	}

	fields = fields.
		Add("pid", ps.Pid()).
		AddFieldIf(ps.Exited(), "exit_code", ps.ExitCode())

	rusage, err := ps.SysUsageUnit()
	if err == nil {
		fields = fields.Add(
			"utime_sec", rusage.Utime.Sec,
			"stime_sec", rusage.Stime.Sec,
			"max_rss_kib", rusage.Maxrss,
		)
	}
	return fields
}

func NullProcessState() ProcessState {
	return &nullProcessState{}
}

type nullProcessState struct{}

func (n *nullProcessState) ExitCode() int {
	return 0
}

func (n *nullProcessState) Exited() bool {
	return true
}

func (n *nullProcessState) Pid() int {
	return 0
}

func (n *nullProcessState) String() string {
	return ""
}

func (n *nullProcessState) Success() bool {
	return true
}

func (n *nullProcessState) SystemTime() time.Duration {
	return 0
}

func (n *nullProcessState) UserTime() time.Duration {
	return 0
}

func (n *nullProcessState) SysUnix() (syscall.WaitStatus, error) {
	return syscall.WaitStatus(0), nil
}

func (n *nullProcessState) SysUsageUnit() (*syscall.Rusage, error) {
	return &syscall.Rusage{}, nil
}
