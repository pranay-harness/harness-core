use clap::Clap;
use std::collections::{HashMap, HashSet};
use strum::IntoEnumIterator;
use strum_macros::EnumIter;

use crate::java_class::JavaClass;
use crate::java_module::{JavaModule, modules};
use std::cmp::Ordering::Equal;

/// A sub-command to analyze the project module targets and dependencies
#[derive(Clap)]
pub struct Analyze {
    /// Filter the reports by affected module module_filter.
    #[clap(short, long)]
    module_filter: Option<String>,
}

#[derive(Debug, Copy, Clone, EnumIter)]
enum Kind {
    CRITICAL,
    ERROR,
    ACTION,
    NOTE,
}

#[derive(Debug)]
struct Report {
    kind: Kind,
    message: String,
    modules: HashSet<String>,
}

pub fn analyze(opts: Analyze) {
    println!("loading...");

    let modules = modules();
    // println!("{:?}", modules);

    let class_modules = modules
        .values()
        .flat_map(|module| {
            module
                .srcs
                .iter()
                .map(|src| (src.1, module))
                .collect::<HashMap<&JavaClass, &JavaModule>>()
        })
        .collect::<HashMap<&JavaClass, &JavaModule>>();

    let classes = class_modules
        .keys()
        .map(|&class| (class.name.clone(), class))
        .collect::<HashMap<String, &JavaClass>>();
    // println!("{:?}", classes);

    if opts.module_filter.is_some() {
        println!("analizing for module {} ...", opts.module_filter.as_ref().unwrap());
    } else {
        println!("analizing...");
    }

    let mut results: Vec<Report> = Vec::new();
    modules.iter().for_each(|tuple| {
        results.extend(check_for_reversed_dependency(tuple.1, &modules));
    });

    class_modules.iter().for_each(|tuple| {
        results.extend(check_already_in_target(tuple.0, tuple.1));
        results.extend(check_for_promotion(
            tuple.0,
            tuple.1,
            &modules,
            &classes,
            &class_modules,
        ));
    });

    let mut total = vec![0, 0, 0, 0];

    results.sort_by(|a, b| {
        let ordering = (a.kind as usize).cmp(&(b.kind as usize));
        if ordering != Equal {
            ordering
        } else {
            a.message.cmp(&b.message)
        }
    });

    results
        .iter()
        .filter(|report| opts.module_filter.is_none() || report.modules.contains(opts.module_filter.as_ref().unwrap()))
        .for_each(|report| {
            println!("{:?}: {}", &report.kind, &report.message);
            total[report.kind as usize] += 1;
        });

    println!();

    for kind in Kind::iter() {
        if total[kind as usize] > 0 {
            println!("{:?} -> {}", kind, total[kind as usize]);
        }
    }
}

fn check_for_reversed_dependency(module: &JavaModule, modules: &HashMap<String, JavaModule>) -> Vec<Report> {
    let mut results: Vec<Report> = Vec::new();

    module.dependencies.iter().for_each(|name| {
        let dependent = modules
            .get(name)
            .expect(&format!("Dependent module {} does not exists", name));

        if module.index >= dependent.index {
            results.push(Report {
                kind: Kind::CRITICAL,
                message: format!(
                    "Module {} depends on module {} that is not lower",
                    module.name, dependent.name
                ),
                modules: [module.name.clone(), dependent.name.clone()].iter().cloned().collect(),
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
                kind: Kind::ACTION,
                message: format!(
                    "{} target module is where it already is - remove the annotation",
                    class.name
                ),
                modules: [module.name.clone()].iter().cloned().collect(),
            })
        }

        results
    }
}

fn check_for_promotion(
    class: &JavaClass,
    module: &JavaModule,
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

        if module.index >= target_module.index {
            results
        } else {
            let mut issue = false;
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
                    results.push(Report {
                        kind: Kind::ERROR,
                        message: format!(
                            "{} depends on {} that is in module {} but {} does not depend on it",
                            class.name, dependent_class.name, dependent_target_module.name, target_module.name
                        ),
                        modules: [dependent_target_module.name.clone(), target_module.name.clone()]
                            .iter()
                            .cloned()
                            .collect(),
                    });
                }

                if dependent_real_module.index < target_module.index {
                    not_ready_yet.push(format!("{} to {}", src, target_module.name));
                }
            });

            if !issue {
                if not_ready_yet.is_empty() {
                    results.push(Report {
                        kind: Kind::ACTION,
                        message: format!("{} is ready to go to {}", class.name, target_module.name),
                        modules: [target_module.name.clone()].iter().cloned().collect(),
                    });
                } else {
                    results.push(Report {
                        kind: Kind::NOTE,
                        message: format!(
                            "{} does not have untargeted dependencies to go to {}. First promote {}",
                            class.name,
                            target_module.name,
                            not_ready_yet.join(", ")
                        ),
                        modules: [target_module.name.clone()].iter().cloned().collect(),
                    });
                }
            }

            results
        }
    }
}
