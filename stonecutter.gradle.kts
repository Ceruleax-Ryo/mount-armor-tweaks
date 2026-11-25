// Stonecutter controller script: runs once for the whole project tree.
stonecutter active "26.3-fabric"

stonecutter parameters {
    val (minecraft, loader) = current.project.split('-', limit = 2)

    // Selects the ["<minecraft>"] and [<loader>."<minecraft>"] tables of stonecutter.properties.toml.
    properties {
        tags(minecraft, loader)
    }

    // Enables `//? if fabric {` / `//? if neoforge {` conditions in sources.
    constants {
        match(loader, "fabric", "neoforge")
    }
}
