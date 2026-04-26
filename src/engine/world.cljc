(ns engine.world
  (:require
   [com.wsscode.pathom3.connect.operation :as pco]
   [com.wsscode.pathom3.interface.eql :as p.eql]
   [engine.entity :as entity]
   [engine.utils :as utils]))

(defn query
  [env args]
  (p.eql/process env args))

(defn query-one
  ([env arg]
   (query-one env {} arg))
  ([env ctx arg]
   (p.eql/process-one env ctx arg)))

(defn query-entity
  ([env entity-id]
   (query-one env {:entity/id entity-id} :entity))
  ([env entity-id arg]
   (query-one env {:entity/id entity-id} arg)))

(defn get-log
  [env]
  (query-one env :world/log))

(defn get-actors-from-entities
  [entities]
  (filter entity/actor? entities))

(defn make-world [entities rooms]
  (let [actor-ids (map :entity/id (get-actors-from-entities entities))
        entities (utils/index-by :entity/id entities)
        initiatives actor-ids]
    {:world/player {:entity/id 0}
     :world/dungeon rooms
     :world/history nil
     :world/initiatives initiatives
     :world/entities entities
     :world/log []}))

(defn with-entities
  [world entities]
  (update world :world/entities merge (utils/index-by :entity/id entities)))

(pco/defresolver world-resolver
  [{:keys [world]} _]
  {::pco/output [:world]}
  {:world (do (prn "get world")
              world)})

(pco/defresolver history-resolver
  [{:keys [world]} _]
  {:world/history (do (prn "get history")
                      (:world/history world))})

(pco/defresolver log-resolver
  [{:keys [world]} _]
  {:world/log (:world/log world)})

(pco/defresolver initiatives-resolver
  [{:keys [world]} _]
  {:world/initiatives (do (prn "get initiatives")
                          (:world/initiatives world))})

(pco/defresolver acting-resolver
  [{:keys [world]} _]
  {:world/acting (do (prn "get acting")
                     (first (:world/initiatives world)))})

(defn update-initiatives
  [world initiatives]
  (assoc world :world/initiatives initiatives))

(pco/defmutation advance-initiative
  [{:keys [world]}]
  (let [initiatives (:world/initiatives world)
        updated-initiatives (if (>= (count initiatives) 2)
                              (concat (rest initiatives) [(first initiatives)])
                              initiatives)
        updated-world (update-initiatives world updated-initiatives)]
    (utils/computation-valid updated-world)))

(def advance-initiative-command
  '(engine.world/advance-initiative))

(defn describe-room
  [room]
  (if room
    (str "Description: " (-> room
                             :room/desc
                             :room/text))
    "The Abyss"))

(pco/defresolver compute-entity-view
  [{:keys [room/room]}]
  {:engine.view/view
   (let [description (describe-room room)]
     {:description description})})

(pco/defmutation add-to-log
  [{:keys [world]} args]
  (let [log (:world/log world)
        updated-log (conj log args)
        updated-world (assoc world :world/log updated-log)]
    (utils/computation-valid updated-world)))

(pco/defresolver actions?
  [{:keys [room/room]}]
  {:world/actions
   (let [{:room/keys [exits]} room]
     ;; (mapcat room/exit-to-action exits)
     )})

(def resolvers [world-resolver
                log-resolver
                advance-initiative
                history-resolver
                initiatives-resolver
                acting-resolver
                compute-entity-view
                add-to-log
                actions?])
