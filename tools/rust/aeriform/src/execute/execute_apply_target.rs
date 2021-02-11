use clap::Clap;

/// An action to be executed
#[derive(Clap)]
pub struct ApplyTarget {
    #[clap(short, long)]
    path: String,

    #[clap(short, long)]
    target: String,
}

pub fn apply_target(opts: ApplyTarget) {

}