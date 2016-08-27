(ns clj-jupyter.util
  (:require [clojure
             [string :as string]
             [walk :as walk]]
            [clojure.data.json :as json]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Data format conversion

(defn snake->kebab [string]
  (string/replace string #"_" "-"))

(defn json->edn [json-string]
  (let [f (fn [[k v]] (if (string? k) [(keyword (snake->kebab k)) v] [k v]))]
    (walk/postwalk (fn [x] (if (map? x) (into {} (map f x)) x))
                   (json/read-str json-string))))

(defn kebab->snake [string]
  (string/replace string #"-" "_"))

(defn edn->json [edn-map]
  (json/write-str
   (let [f (fn [[k v]] (if (keyword? k) [(kebab->snake (name k)) v] [k v]))]
     (walk/postwalk (fn [x] (if (map? x) (into {} (map f x)) x)) edn-map))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Time

(def iso-fmt (java.text.SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSSS"))

(defn now []
  (.format iso-fmt (java.util.Date.)))
