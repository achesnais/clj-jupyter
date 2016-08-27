all:
	lein do clean, uberjar
	cp -f target/IClojure.jar ~/Library/Jupyter/kernels/clojure/IClojure.jar
	sed 's|HOME|'${HOME}'|' resources/clj_jupyter/kernel.json > ~/Library/Jupyter/kernels/clojure/kernel.json
