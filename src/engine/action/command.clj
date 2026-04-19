(ns engine.action.command
  (:require [engine.entity :as entity]
            [engine.utils :refer [mutation] :as utils]))

(defn teleport
  [entity-id location]
  (mutation `entity/update-entity-location
            {:entity/id entity-id :entity/location location}))
