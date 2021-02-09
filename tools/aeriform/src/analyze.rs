use clap::Clap;
use std::cmp::Ordering::Equal;
use std::collections::{HashMap, HashSet};
use strum::IntoEnumIterator;
use strum_macros::EnumIter;

use crate::java_class::{JavaClass, JavaClassTraits};
use crate::java_module::{modules, JavaModule};

/// A sub-command to analyze the project module targets and dependencies
#[derive(Clap)]
pub struct Analyze {
    /// Filter the reports by affected module module_filter.
    #[clap(short, long)]
    module_filter: Option<String>,

    /// Filter the reports by affected module root root_filter.
    #[clap(short, long)]
    root_filter: Option<String>,

    #[clap(short, long)]
    auto_actionable_filter: bool,

    #[clap(short, long)]
    auto_actionable_command: bool,
}

#[derive(Debug, Copy, Clone, EnumIter)]
enum Kind {
    Critical,
    Error,
    AutoAction,
    DevAction,
    Blocked,
    ToDo,
}

#[derive(Debug)]
struct Report {
    kind: Kind,
    message: String,
    action: String,

    for_class: String,
    indirect_classes: HashSet<String>,

    for_modules: HashSet<String>,
}

pub fn analyze(opts: Analyze) {
    println!("loading...");

    let modules = modules();
    // println!("{:?}", modules);

    let mut class_modules: HashMap<&JavaClass, &JavaModule> = HashMap::new();

    modules.values().for_each(|module| {
        module.srcs.iter().for_each(|src| match class_modules.get(src.1) {
            None => {
                class_modules.insert(src.1, module);
            }
            Some(&current) => {
                if current.index < module.index {
                    class_modules.insert(src.1, module);
                }
            }
        });
    });

    let classes = class_modules
        .keys()
        .map(|&class| (class.name.clone(), class))
        .collect::<HashMap<String, &JavaClass>>();
    // println!("{:?}", classes);

    if opts.module_filter.is_some() {
        println!("analizing for module {} ...", opts.module_filter.as_ref().unwrap());
    } else if opts.root_filter.is_some() {
        println!("analizing for root {} ...", opts.root_filter.as_ref().unwrap());
    } else {
        println!("analizing...");
    }

    let mut results: Vec<Report> = Vec::new();
    modules.iter().for_each(|tuple| {
        results.extend(check_for_classes_in_more_than_one_module(tuple.1, &class_modules));
        results.extend(check_for_reversed_dependency(tuple.1, &modules));
    });

    class_modules.iter().for_each(|tuple| {
        results.extend(check_already_in_target(tuple.0, tuple.1));
        results.extend(check_for_extra_break(tuple.0, tuple.1));
        results.extend(check_for_moves(tuple.0, &modules, &classes, &class_modules));
        results.extend(check_for_deprecated_module(tuple.0, tuple.1));
    });

    let mut total = vec![0, 0, 0, 0, 0, 0];

    results.sort_by(|a, b| {
        let ordering = (a.kind as usize).cmp(&(b.kind as usize));
        if ordering != Equal {
            ordering
        } else {
            a.message.cmp(&b.message)
        }
    });

    println!("Detecting indirectly involved classes...");

    let indirect_classes: &mut HashSet<&String> = &mut HashSet::new();
    loop {
        let original: HashSet<&String> = indirect_classes.iter().map(|&s| s).collect();

        results
            .iter()
            .filter(|&report| filter_report(&opts, report) || original.contains(&report.for_class))
            .for_each(|report| {
                indirect_classes.extend(&report.indirect_classes);
            });

        if original.len() == indirect_classes.len() {
            break;
        }
    }

    results
        .iter()
        .filter(|&report| filter_report(&opts, report) || indirect_classes.contains(&report.for_class))
        .filter(|&report| filter_by_auto_actionable(&opts, report))
        .for_each(|report| {
            println!("{:?}: {}", &report.kind, &report.message);
            if opts.auto_actionable_command && !report.action.is_empty() {
                println!("   {}", &report.action);
            }
            total[report.kind as usize] += 1;
        });

    println!();

    for kind in Kind::iter() {
        if total[kind as usize] > 0 {
            println!("{:?} -> {}", kind, total[kind as usize]);
        }
    }
}

fn filter_report(opts: &Analyze, report: &Report) -> bool {
    filter_by_module(&opts, report) && filter_by_root(&opts, report)
}

fn filter_by_auto_actionable(opts: &Analyze, report: &Report) -> bool {
    !opts.auto_actionable_filter || !report.action.is_empty()
}

fn filter_by_root(opts: &Analyze, report: &Report) -> bool {
    opts.root_filter.is_none()
        || report.for_modules.iter().any(|name| {
            let root = opts.root_filter.as_ref().unwrap();
            name.starts_with(root) && name.chars().nth(root.len()).unwrap() == ':'
        })
}

fn filter_by_module(opts: &Analyze, report: &Report) -> bool {
    opts.module_filter.is_none() || report.for_modules.contains(opts.module_filter.as_ref().unwrap())
}

fn check_for_extra_break(class: &JavaClass, module: &JavaModule) -> Vec<Report> {
    let mut results: Vec<Report> = Vec::new();

    class
        .break_dependencies_on
        .iter()
        .filter(|&break_dependency| !class.dependencies.contains(break_dependency))
        .for_each(|break_dependency| {
            let mut modules: HashSet<String> = HashSet::new();
            modules.insert(module.name.clone());
            if class.target_module.is_some() {
                modules.insert(class.target_module.as_ref().unwrap().clone());
            }

            results.push(Report {
                kind: Kind::Critical,
                message: format!("{} has no dependency on {}", class.name, break_dependency),
                action: Default::default(),
                for_class: class.name.clone(),
                indirect_classes: Default::default(),
                for_modules: modules,
            })
        });

    results
}

fn check_for_classes_in_more_than_one_module(
    module: &JavaModule,
    classes: &HashMap<&JavaClass, &JavaModule>,
) -> Vec<Report> {
    let mut results: Vec<Report> = Vec::new();

    module
        .srcs
        .values()
        .filter(|class| class.location.ne("n/a"))
        .for_each(|class| {
            let tracked_module = classes.get(class).unwrap();
            if tracked_module.name.ne(&module.name) {
                results.push(Report {
                    kind: Kind::Critical,
                    message: format!("{} appears in {} and {}", class.name, module.name, tracked_module.name),
                    action: Default::default(),
                    for_class: class.name.clone(),
                    indirect_classes: Default::default(),
                    for_modules: [module.name.clone(), tracked_module.name.clone()]
                        .iter()
                        .cloned()
                        .collect(),
                });
            }
        });

    results
}

fn check_for_reversed_dependency(module: &JavaModule, modules: &HashMap<String, JavaModule>) -> Vec<Report> {
    let mut results: Vec<Report> = Vec::new();

    module.dependencies.iter().for_each(|name| {
        let dependent = modules
            .get(name)
            .expect(&format!("Dependent module {} does not exists", name));

        if module.index >= dependent.index {
            results.push(Report {
                kind: Kind::Critical,
                message: format!(
                    "Module {} depends on module {} that is not lower",
                    module.name, dependent.name
                ),
                action: Default::default(),
                for_class: Default::default(),
                indirect_classes: Default::default(),
                for_modules: [module.name.clone(), dependent.name.clone()].iter().cloned().collect(),
            });
        }
    });

    results
}

fn check_already_in_target(class: &JavaClass, module: &JavaModule) -> Vec<Report> {
    let mut results: Vec<Report> = Vec::new();

    let target_module = class.target_module.as_ref();
    if target_module.is_none() {
        results
    } else {
        if module.name.eq(target_module.unwrap()) {
            results.push(Report {
                kind: Kind::AutoAction,
                message: format!(
                    "{} target module is where it already is - remove the annotation",
                    class.name
                ),
                action: Default::default(),
                for_class: class.name.clone(),
                indirect_classes: Default::default(),
                for_modules: [module.name.clone()].iter().cloned().collect(),
            })
        }

        results
    }
}

fn check_for_moves(
    class: &JavaClass,
    modules: &HashMap<String, JavaModule>,
    classes: &HashMap<String, &JavaClass>,
    class_modules: &HashMap<&JavaClass, &JavaModule>,
) -> Vec<Report> {
    let mut results: Vec<Report> = Vec::new();

    let target_module_name = class.target_module.as_ref();
    if target_module_name.is_none() {
        results
    } else {
        let target_module = modules.get(target_module_name.unwrap()).unwrap();

        let mut issue = false;
        let mut all_classes: HashSet<String> = HashSet::new();
        let mut not_ready_yet = Vec::new();
        class.dependencies.iter().for_each(|src| {
            let &dependent_class = classes
                .get(src)
                .expect(&format!("The source {} is not find in any module", src));

            let &dependent_real_module = class_modules.get(dependent_class).expect(&format!(
                "The class {} is not find in the modules",
                dependent_class.name
            ));

            let dependent_target_module = if dependent_class.target_module.is_some() {
                modules.get(dependent_class.target_module.as_ref().unwrap()).unwrap()
            } else {
                dependent_real_module
            };

            if !target_module.name.eq(&dependent_target_module.name)
                && !target_module.dependencies.contains(&dependent_target_module.name)
            {
                issue = true;
                let mdls = [dependent_target_module.name.clone(), target_module.name.clone()]
                    .iter()
                    .cloned()
                    .collect();
                let indirect_classes = [dependent_class.name.clone()].iter().cloned().collect();
                results.push(if class.break_dependencies_on.contains(src) {
                    Report {
                        kind: Kind::DevAction,
                        message: format!(
                            "{} depends on {} and this dependency has to be broken",
                            class.name, dependent_class.name
                        ),
                        action: Default::default(),
                        for_class: class.name.clone(),
                        indirect_classes: indirect_classes,
                        for_modules: mdls,
                    }
                } else {
                    Report {
                        kind: Kind::Error,
                        message: format!(
                            "{} depends on {} that is in module {} but {} does not depend on it",
                            class.name, dependent_class.name, dependent_target_module.name, target_module.name
                        ),
                        action: Default::default(),
                        for_class: class.name.clone(),
                        indirect_classes: indirect_classes,
                        for_modules: mdls,
                    }
                });
            }

            if dependent_real_module.index < target_module.index {
                all_classes.insert(src.clone());
                not_ready_yet.push(format!("{} to {}", src, target_module.name));
            }
        });

        if !issue {
            let mdls = [target_module.name.clone()].iter().cloned().collect();

            if not_ready_yet.is_empty() {
                let module = class_modules.get(class);

                let msg = format!("{} is ready to go to {}", class.name, target_module.name);

                results.push(match module {
                    None => Report {
                        kind: Kind::DevAction,
                        action: Default::default(),
                        message: msg,
                        for_class: class.name.clone(),
                        indirect_classes: Default::default(),
                        for_modules: mdls,
                    },
                    Some(&module) => Report {
                        kind: Kind::AutoAction,
                        message: msg,
                        action: format!(
                            "execute move-class --from-module=\"{}\" --from-location=\"{}\" --to-module=\"{}\"",
                            module.directory,
                            class.relative_location(),
                            target_module.directory
                        ),
                        for_class: class.name.clone(),
                        indirect_classes: Default::default(),
                        for_modules: mdls,
                    },
                });
            } else {
                all_classes.insert(class.name.clone());

                results.push(Report {
                    kind: Kind::Blocked,
                    message: format!(
                        "{} does not have untargeted dependencies to go to {}. First move {}",
                        class.name,
                        target_module.name,
                        not_ready_yet.join(", ")
                    ),
                    action: Default::default(),
                    for_class: class.name.clone(),
                    indirect_classes: all_classes,
                    for_modules: mdls,
                });
            }
        }

        results
    }
}

fn check_for_deprecated_module(class: &JavaClass, module: &JavaModule) -> Vec<Report> {
    let mut results: Vec<Report> = Vec::new();

    if class.target_module.is_none() && module.deprecated {
        results.push(Report {
            kind: Kind::ToDo,
            message: format!(
                "{} is in deprecated module {} and has no target module",
                class.name, module.name
            ),
            action: Default::default(),
            for_class: class.name.clone(),
            indirect_classes: Default::default(),
            for_modules: [module.name.clone()].iter().cloned().collect(),
        });
    }

    results
}
