# Fluidic Arm

[![MCVersion](https://img.shields.io/badge/Available%20for-MC%201.20.1%20-informational)](https://www.curseforge.com/minecraft/mc-mods/fluidic-arm) [![License](https://img.shields.io/badge/License-LGPLv3-orange.svg?style=flat-square)](LICENSE)
---

## About

A [GregTech: CEu Modern](https://www.curseforge.com/minecraft/mc-mods/gregtechceu-modern) addon that introduces a new `Fluidic Arm` cover.

See [Releases](https://github.com/Cyber1551/Fluidic-Arm/releases) for the compiled jars.

<img src="/img/icons.png" alt="image" width="500">

## Features

The `Fluidic Arm` cover combines the functionality of a `Robotic Arm` and a `Fluid Regulator`. 


It allows transfer of both **items** and **fluids** between machines.

Additionally, some minor UI tweaks have been made for a more consistent experience.

<img src="/img/robotarm.png" alt="image" width="250"> <img src="/img/fluidregulator.png" alt="image" width="250">
<br />
<img src="/img/robotarm_btn.png" alt="image" width="250"> <img src="/img/fluidregulator_btn.png" alt="image" width="250">


 
## Config

The `Fluidic Arm` cover can be configured to use different recipe types.

```
general {
    # Recipe Type. Options: none (no generated recipes), easy (2x2 crafting), normal (3x3 crafting). Default: normal
    S:recipeType=normal
}
```


The default recipe type is `normal` which follows the same pattern as gregtech; a 3x3 crafting recipe as well as assembler and assembly line recipes with necessary research. 

<img src="/img/3x3.png" alt="image" width="250"><br /><img src="/img/assemblyline.png" alt="image" width="250">

The `easy` option generates a recipe that can be crafted in a 2x2 crafting grid or an assembler.

<img src="/img/2x2.png" alt="image" width="250">

The `none` option disables all recipes for the cover. Useful if you are making a custom pack and want to add your own recipes.

## Requirements

- Minecraft **1.20.1** (Forge)
- [GregTech CEu Modern](https://www.curseforge.com/minecraft/mc-mods/gregtechceu-modern)

## License

Licensed under [LGPL-3.0](LICENSE), matching GregTech CEu Modern.
