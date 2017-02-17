all:
	lein do clean, uberjar
	# further os specific locations: https://jupyter-client.readthedocs.io/en/latest/kernels.html#kernel-specs
	./install.sh
