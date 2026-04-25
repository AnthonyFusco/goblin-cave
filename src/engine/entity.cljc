(ns engine.entity
  (:require
   [com.wsscode.pathom3.connect.operation :as pco]
   [engine.utils :as utils]))

(defn make-entity [id coords location state]
  {:entity/id id
   :entity/coords coords
   :entity/location location
   :entity/state state
   :entity/rule-type :entity})

(defn make-coords [x y z]
  {:entity/x x :entity/y y :entity/z z})

(def player
  (make-entity 0 (make-coords 0 0 0) 0 {:name "Player"}))

(def other
  (make-entity 1 (make-coords 1 1 1) 1 {:name "Other"}))

(defn get-name
  [entity]
  (get-in entity [:entity/state :name]))

(pco/defresolver entity->id
  [{:keys [entity]}]
  {:entity/id (do (prn "entity->id")
                  (:entity/id entity))})

(pco/defresolver entity->coords
  [{:keys [entity]}]
  {:entity/coords (do (prn "entity to coords ")
                      (:entity/coords entity))})

(pco/defresolver id->location
  [{:keys [entity entity/id]}]
  {::pco/input [:entity :entity/id]
   ::pco/output [:entity/location]}
  {:entity/location
   (do (prn (str "id->location" " id:" id))
       (:entity/location entity))})

(pco/defresolver id->coords
  [{:keys [world/entities]} {:keys [entity/id]}]
  {::pco/input [:world/entities [:entity/id :entity/coords]]}
  {:entity/coords
   (do (prn (str "id->coords" " id:" id))
       (:entity/coords (get entities id)))})

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

(pco/defresolver entity-resolver
  [{:keys [entity/id world/entities]}]
  {:entity (do (prn "entity resolver")
               (first (filter #(= (:entity/id %) id) entities)))})

(pco/defresolver entities-resolver
  [env {:keys [world]}]
  {::pco/output [{:world/entities [:entity]}]}
  {:world/entities
   (do (prn "entities resolver")
       (let [entities (vals (:world/entities world))
             location (pco/params env)]
         (if (seq location)
           (->> entities
                (filter #(= (:entity/location %) (:entity/location location))))
           entities)))})

(def resolvers [entity-resolver
                entity->id
                entity->coords
                entities-resolver
                id->location
                update-entity-location
                id->coords])
