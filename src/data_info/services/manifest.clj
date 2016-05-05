(ns data-info.services.manifest
  (:use [data-info.services.sharing :only [anon-file-url anon-readable?]]
        [clj-jargon.init :only [with-jargon]]
        [clj-jargon.metadata :only [get-attribute attribute?]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.tools.logging :as log]
            [clojure-commons.file-utils :as ft]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [data-info.util.validators :as validators]
            [data-info.services.stat :as stat]
            [data-info.services.uuids :as uuids]
            [data-info.util.irods :as irods]
            [data-info.util.logging :as dul]
            [tree-urls-client.core :as tree]
            [data-info.util.config :as cfg])
  (:import [org.apache.tika Tika]))

(def ^:private coge-attr "ipc-coge-link")

(defn- extract-tree-urls
  [cm fpath]
  (if (attribute? cm fpath (cfg/tree-urls-attr))
    (-> (get-attribute cm fpath (cfg/tree-urls-attr))
      first
      :value
      ft/basename
      tree/get-tree-urls
      :tree-urls)
    []))

(defn- extract-coge-view
  [cm fpath]
  (if (attribute? cm fpath coge-attr)
    (mapv (fn [{url :value} idx] {:label (str "gene_" idx) :url url})
          (get-attribute cm fpath coge-attr) (range))
    []))

(defn- format-anon-files-url
  [fpath]
  {:label "anonymous" :url (anon-file-url fpath)})

(defn- extract-urls
  [cm fpath]
  (let [urls (concat (extract-tree-urls cm fpath) (extract-coge-view cm fpath))]
    (vec (if (anon-readable? cm fpath)
           (conj urls (format-anon-files-url fpath))
           urls))))

(defn- manifest-map
  [cm user {:keys [path] :as file}]
  (-> (select-keys file [:content-type :infoType])
      (assoc :action "manifest"
             :urls (extract-urls cm path))))

(defn- manifest
  [cm user file]
  (log/warn file)
  (let [path (ft/rm-last-slash (:path file))]
    (validators/user-exists cm user)
    (validators/path-exists cm path)
    (validators/path-is-file cm path)
    (validators/path-readable cm user path)
    (manifest-map cm user file)))

(defn do-manifest-uuid
  [user data-id]
  (with-jargon (cfg/jargon-cfg) [cm]
    (let [file (uuids/path-for-uuid cm user data-id)]
      (manifest cm user file))))

(with-pre-hook! #'do-manifest-uuid
  (fn [user data-id]
    (dul/log-call "do-manifest-uuid" user data-id)))

(with-post-hook! #'do-manifest-uuid (dul/log-func "do-manifest-uuid"))