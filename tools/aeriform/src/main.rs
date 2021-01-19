mod java_class;
mod java_module;

use crate::java_class::JavaClass;
use crate::java_module::{modules, JavaModule};
use std::collections::HashMap;

fn main() {
    println!("loading...");

    let modules = modules();
    //println!("{:?}", modules);

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
    //println!("{:?}", classes);

    println!("analizing...");
    class_modules.iter().for_each(|tuple| {
        check_already_in_target(tuple.0, tuple.1);
        check_for_promotion(tuple.0, tuple.1, &modules, &classes, &class_modules);
    });
}

fn check_already_in_target(class: &JavaClass, module: &JavaModule) {
    let target_module = class.target_module.as_ref();
    if target_module.is_none() {
        return;
    }

    if module.name.eq(target_module.unwrap()) {
        println!("WARNING: {} target module is where it already is", class.name)
    }
}

fn check_for_promotion(
    class: &JavaClass,
    module: &JavaModule,
    modules: &HashMap<String, JavaModule>,
    classes: &HashMap<String, &JavaClass>,
    class_modules: &HashMap<&JavaClass, &JavaModule>,
) {
    let target_module_name = class.target_module.as_ref();
    if target_module_name.is_none() {
        return;
    }

    let target_module = modules.get(target_module_name.unwrap()).unwrap();

    if module.index >= target_module.index {
        return;
    }

    //println!("INFO: {:?}", class);

    let mut issue = false;
    class.dependencies.iter().for_each(|src| {
        let &dependent_class = classes
            .get(src)
            .expect(&format!("The source {} is not find in any module", src));

        let dependent_module = if dependent_class.target_module.is_some() {
            modules.get(dependent_class.target_module.as_ref().unwrap()).unwrap()
        } else {
            class_modules.get(dependent_class).expect(&format!(
                "The class {} is not find in the modules",
                dependent_class.name
            ))
        };

        //println!("INFO: {:?} depends on {:?} that is in module that is higher than the target {:?}", class, dependent_class, dependent_module);

        if dependent_module.index < target_module.index {
            issue = true;
            println!(
                "ERROR: {} depends on {} that is in module {} higher than the target {}",
                class.name, dependent_class.name, dependent_module.name, target_module.name
            )
        }
    });

    if !issue {
        println!("WARNING: {} is set and nothing prevents the promotion", class.name)
    }
}
