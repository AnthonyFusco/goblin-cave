(ns engine.world
  (:require
   [com.wsscode.pathom3.connect.operation :as pco]
   [com.wsscode.pathom3.interface.eql :as p.eql]
   [engine.utils :as utils]))

(defn query
  [env args]
  (p.eql/process env args))

(defn query-one
  ([env arg]
   (query-one env {} arg))
  ([env ctx arg]
   (p.eql/process-one env ctx arg)))

(defn make-world [entities rooms]
  (let [entity-ids (map :entity/id entities)
        entities (utils/index-by :entity/id entities)
        initiatives entity-ids]
    {:world/player {:entity/id 0}
     :world/dungeon rooms
     :world/history nil
     :world/initiatives initiatives
     :world/entities entities}))

(pco/defresolver world-resolver
  [{:keys [world]} _]
  {::pco/output [:world]}
  {:world (do (prn "get world")
              world)})

(pco/defresolver history-resolver
  [{:keys [world]} _]
  {:world/history (do (prn "get history")
                 (:world/history world))})

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

(pco/defresolver actions?
  [{:keys [room/room]}]
  {:world/actions
   (let [{:room/keys [exits]} room]
     ;; (mapcat room/exit-to-action exits)
     )})

(def resolvers [world-resolver
                advance-initiative
                history-resolver
                initiatives-resolver
                acting-resolver
                compute-entity-view
                actions?])
