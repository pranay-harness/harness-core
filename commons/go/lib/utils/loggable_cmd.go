// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package utils

import (
	"bytes"
	"github.com/pkg/errors"
	"strings"

	"mvdan.cc/sh/v3/syntax"
)

// GetLoggableCmd accepts a shell command and makes it loggable.
func GetLoggableCmd(cmd string) (string, error) {
	parser := syntax.NewParser(syntax.KeepComments(false))
	printer := syntax.NewPrinter(syntax.Minify(false))

	r := strings.NewReader(cmd)
	prog, err := parser.Parse(r, "")
	if err != nil {
		return "", errors.Wrap(err, "failed to parse command")
	}

	var stmts []*syntax.Stmt
	for _, stmt := range prog.Stmts {
		// convert the statement to a string and then encode special characters.
		var buf bytes.Buffer
		if printer.Print(&buf, stmt); err != nil {
			return "", errors.Wrap(err, "failed to parse statement")
		}

		// create a new statement that echos the
		// original shell statement.
		echo := &syntax.Stmt{
			Cmd: &syntax.CallExpr{
				Args: []*syntax.Word{
					{
						Parts: []syntax.WordPart{
							&syntax.Lit{
								Value: "echo",
							},
						},
					},
					{
						Parts: []syntax.WordPart{
							&syntax.SglQuoted{
								Dollar: false,
								Value:  "--- " + buf.String(),
							},
						},
					},
				},
			},
		}
		// append the echo statement and the statement
		stmts = append(stmts, echo)
		stmts = append(stmts, stmt)
	}
	// replace original statements with new statements
	prog.Stmts = stmts

	buf := new(bytes.Buffer)
	printer.Print(buf, prog)
	return buf.String(), nil
}
