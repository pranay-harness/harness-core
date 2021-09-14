// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package stream

import (
	"encoding/json"
	"os"

	client "github.com/wings-software/portal/product/log-service/client"

	"gopkg.in/alecthomas/kingpin.v2"
)

type infoCommand struct {
	server string
	token  string
}

func (c *infoCommand) run(*kingpin.ParseContext) error {
	client := client.NewHTTPClient(c.server, "test", c.token, false)
	info, err := client.Info(nocontext)
	if err != nil {
		return err
	}
	enc := json.NewEncoder(os.Stdout)
	enc.SetIndent("", "  ")
	return enc.Encode(info)
}

func registerInfo(app *kingpin.CmdClause) {
	c := new(infoCommand)

	cmd := app.Command("info", "return stream data").
		Action(c.run)

	cmd.Flag("server", "server endpoint").
		Default("http://localhost:8079").
		StringVar(&c.server)

	cmd.Arg("token", "server token").
		Required().
		StringVar(&c.token)
}
