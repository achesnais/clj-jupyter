all:
	lein do clean, uberjar
	mkdir -p ~/.ipython/kernels/clj-jupyter
	cp -f target/IClojure.jar ~/.ipython/kernels/clj-jupyter/IClojure.jar
	sed 's|HOME|'${HOME}'|' resources/clj_jupyter/kernel.json > ~/.ipython/kernels/clj-jupyter/kernel.json
