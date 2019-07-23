(ns data-info.routes.schemas.common
  (:use [common-swagger-api.schema :only [describe NonBlankString ->optional-param]])
  (:require [common-swagger-api.schema.data :as data-schema]
            [schema.core :as s])
  (:import [java.util UUID]))

(defn get-error-code-block
  [& error-codes]
  (str "

#### Error Codes:
    " (clojure.string/join "\n    " error-codes)))

(def DataIdPathParam data-schema/DataIdPathParam)

(def Paths data-schema/Paths)

(s/defschema OptionalPaths
  {(s/optional-key :paths) (describe [NonBlankString] "A list of iRODS paths")})

(s/defschema DataIds
  {:ids (describe [UUID] "A list of iRODS data-object UUIDs")})

(s/defschema OptionalPathsOrDataIds
  (-> (merge DataIds OptionalPaths)
      (->optional-param :ids)))

(def PermissionEnum data-schema/PermissionEnum)
