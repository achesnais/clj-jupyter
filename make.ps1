lein do clean, uberjar
cp --force target/IClojure.jar $env:APPDATA\jupyter\kernels\clojure\IClojure.jar
(Get-Content resources/clj_jupyter/kernel.json) -replace 'HOME', $HOME | Set-Content $env:APPDATA\jupyter\kernels\clojure\kernel.json
