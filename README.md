# CLJ-Jupyter

**UPDATE 2017-02-12: I actually won't have time to maintain this project for the foreseeable future. Feel free to fork.** 

This is a basic implementation of a Clojure kernel for Jupyter. It should in theory support most of what you'd expect of a basic Clojure Jupyter experience.

You can read [this](http://achesnais.github.io/clojure/jupyter/2016/08/27/introducing-clj-jupyter.html) if you're wondering why I chose to build my own.

## Usage

### Vanilla kernel

Navigate to the root of the project and run `make` to compile the uberjar and set it up in the relevant folder. Unfortunately this process will only work on Macs for now, but I'm happy to take pull requests to implement the process for other OSes.

Once the build/install is done, Jupyter should now offer a Clojure kernel as an option when creating a notebook.

### Custom kernel

You can of course add any dependency you want, or add new namespaces to be able to reference them from your notebook.

## Outstanding issues

* I'm still trying to figure out how Kernel restart is supposed to work (currently if you try and use "Restart" from the Jupyter interface, you'll get an error saying the kernel died).

## Roadmap

* Ensure a stable vanilla implementation, compliant with the basic Jupyter messaging protocol.
* Extend the REPL evaluation to display data in different Jupyter compliant formats (tables, plots, etc.)
* Develop a lein plugin to allow for any library or project to be turned into a Jupyter kernel.

## Inspirations

This project would not have been possible without the work/proof of concept done by Rory Kirchner on his [Clojupyter](https://github.com/roryk/clojupyter/) project.

## License

Copyright © 2016 Antoine Chesnais

Distributed under the Eclipse Public License version 1.0.
