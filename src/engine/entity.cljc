(ns engine.entity
  (:require
   [com.wsscode.pathom3.connect.operation :as pco]
   [medley.core :refer [deep-merge]]
   [engine.utils :as utils]))

(defn make-entity [id location state description & [properties]]
  {:entity/id id
   :entity/location location
   :entity/state state
   :entity/rule-type :entity
   :entity/properties (or properties #{:actor})
   :entity/description description})

(defn actor?
  [entity]
  (contains? (:entity/properties entity) :actor))

(defn make-coords [x y z]
  {:entity/x x :entity/y y :entity/z z})

(def player
  (make-entity 0 0 {:name "Player"} "You"))

(def other
  (make-entity 1 1 {:name "Other"} "The other guy"))

(defn get-name
  [entity]
  (get-in entity [:entity/state :name]))

(defn get-description
  [entity]
  (get-in entity [:entity/description]))

(pco/defresolver entity->id
  [{:keys [entity]}]
  {:entity/id (do (prn "entity->id")
                  (:entity/id entity))})

(pco/defresolver id->location
  [{:keys [entity entity/id]}]
  {::pco/input [:entity :entity/id]
   ::pco/output [:entity/location]}
  {:entity/location
   (do (prn (str "id->location" " id:" id))
       (:entity/location entity))})

(defn update-entity-location-fn [entity location]
  (merge entity {:entity/location location}))

(defn update-entity [world entity]
  (assoc-in world [:world/entities (:entity/id entity)] entity))

(pco/defmutation update-entity-location
  [{:keys [world]} {:keys [entity entity/location]}]
  {::pco/input [:entity :entity/location]}
  (let [updated-entity (update-entity-location-fn entity location)
        updated-world (update-entity world updated-entity)]
    (utils/computation-valid updated-world)))

(pco/defmutation mutate-entity
  [{:keys [world]} {:keys [entity action/mutation]}]
  {::pco/input [:entity :action/mutation]}
  (let [updated-entity (deep-merge entity mutation)
        updated-world (update-entity world updated-entity)]
    (utils/computation-valid updated-world)))

(pco/defresolver entity-resolver
  [{:keys [entity/id world/entities]}]
  {:entity (do (prn "entity resolver")
               (first (filter #(= (:entity/id %) id) entities)))})

(pco/defresolver entities-resolver
    "Also resolves entities per location"
    [env {:keys [world]}]
    {::pco/output [{:world/entities [:entity]}]}
    {:world/entities
     (do (prn "entities resolver")
         (let [entities (vals (:world/entities world))
               location (pco/params env)]
           (if (seq location)
             (do (prn "resolving entities per location")
                 (->> entities
                      (filter #(= (:entity/location %) (:entity/location location)))))
             entities)))})

(pco/defresolver entities-in-location
  [{:keys [entity/location world/entities]}]
  {::pco/output [{:entities-in-location [:entity]}]}
  {:entities-in-location (do
                         (prn "entities in location resolver")
                         (->> entities
                              (filter #(= (:entity/location %) location))))})

(def resolvers [entity-resolver
                entity->id
                entities-in-location
                entities-resolver
                id->location
                update-entity-location
                mutate-entity])
