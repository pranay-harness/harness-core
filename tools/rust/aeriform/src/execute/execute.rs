use clap::Clap;

use crate::execute_class_move::{move_class, MoveClass};
use crate::execute_apply_target::{apply_target, ApplyTarget};

pub const MODULE_IMPORT: &str = "import io.harness.annotations.dev.Module;";
pub const TARGET_MODULE_IMPORT: &str = "import io.harness.annotations.dev.TargetModule;";

#[derive(Clap)]
enum Action {
    #[clap(version = "1.0", author = "George Georgiev <george@harness.io>")]
    MoveClass(MoveClass),

    #[clap(version = "1.0", author = "George Georgiev <george@harness.io>")]
    ApplyTarget(ApplyTarget)
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
        Action::ApplyTarget(action_opts) => {
            apply_target(action_opts);
        }
    }
}
