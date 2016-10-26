all:
	lein do clean, uberjar
	# further os specific locations: https://jupyter-client.readthedocs.io/en/latest/kernels.html#kernel-specs
	unamestr=$(uname)
	if [[ "$unamestr" == 'Linux' ]]; then
		cp -f target/IClojure.jar ~/.local/share/jupyter/kernels/clojure/IClojure.jar
		sed 's|HOME|'${HOME}'|' resources/clj_jupyter/kernel.json > ~/.local/share/jupyter/kernels/clojure/kernel.json
	elif [[ "$unamestr" == 'Darwin' ]]; then
		cp -f target/IClojure.jar ~/Library/Jupyter/kernels/clojure/IClojure.jar
		sed 's|HOME|'${HOME}'|' resources/clj_jupyter/kernel.json > ~/Library/Jupyter/kernels/clojure/kernel.json
	fi

	
