(ns engine.action.command
  (:require [engine.entity :as entity]
            [engine.utils :refer [mutation] :as utils]
            [engine.world :as world]))

(defn teleport
  [entity-id location]
  (mutation `entity/update-entity-location
            {:entity/id entity-id :entity/location location}))

(defn mutate-entity
  [entity-id mutation-args]
  (mutation `entity/mutate-entity
            {:entity/id entity-id :action/mutation {:entity/state mutation-args}}))

(defn make-show-command [args]
  (mutation `world/add-to-log args))