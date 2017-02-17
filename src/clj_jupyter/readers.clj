(ns clj-jupyter.readers
  (:require [clojure.pprint :as pprint]
            [clojure.string :as str]))

;; Just print it and return the closest you can get to nil
;; This does mean that the caller is obliged to only print the real (quote nil)
;; What about nested literals??
(defn unknown-literal
  [tag v]
  (println (str/join "" ["#" tag " " (pr-str v)]))
  '(quote nil))
