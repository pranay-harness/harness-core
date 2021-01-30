use crate::execute_class_move::{move_class, MoveClass};
use clap::Clap;

#[derive(Clap)]
enum Action {
    #[clap(version = "1.0", author = "George Georgiev <george@harness.io>")]
    MoveClass(MoveClass),
}

/// A sub-command to analyze the project module targets and dependencies
#[derive(Clap)]
pub struct Execute {
    /// Filter the reports by affected module module_filter.
    #[clap(subcommand)]
    action: Action,
}

pub fn execute(opts: Execute) {
    match opts.action {
        Action::MoveClass(action_opts) => {
            move_class(action_opts);
        }
    }
}
