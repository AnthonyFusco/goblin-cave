(ns engine.core
  (:require
   [com.wsscode.pathom3.connect.built-in.resolvers :as pbir]
   [com.wsscode.pathom3.connect.built-in.plugins :as pbip]
   [com.wsscode.pathom3.connect.indexes :as pci]
   [com.wsscode.pathom3.connect.operation :as pco]
   [com.wsscode.pathom3.interface.eql :as p.eql]
   [com.wsscode.pathom3.interface.smart-map :as psm]
   [com.wsscode.pathom3.plugin :as p.plugin]
   [engine.room :as room]
   [engine.entity :as entity]
   [engine.action :as action]
   [engine.semantic :as semantic]
   [engine.object :as object]
   [engine.world :as world]
   [engine.action.command :as command]
   [engine.utils :as utils]))

(def initial-entities
  (flatten [entity/other entity/player object/objects]))

(def initial-world
  (world/make-world initial-entities room/rooms))

(defn greet
  "Callable entry point to the application."
  [data]
  (str "Hello, " (or data "World") "!"))

(defn with-world
  [env world]
  (assoc env :world world))

(def indexes (-> (pci/register [entity/resolvers
                                room/resolvers
                                world/resolvers])
                 (p.plugin/register pbip/mutation-resolve-params)))

(def env (-> indexes
             (with-world initial-world)))

(defn with-entities
  [env entities]
  (update env :world world/with-entities entities))

(defn with-history
  [env mutation]
  (update-in env [:world :world/history] conj mutation))

(defn process
  [env mutation]
  (let [computation (world/query-one env mutation)
        {:keys [result]} computation]
    (when (utils/computation-valid? computation)
      (-> env
          (with-world result)
          (with-history mutation)))))

(defn get-history
  [env]
  (world/query-one env :world/history))

(defn get-world
  [env]
  (world/query-one env :world))

(defn get-location
  [env player-id]
  (world/query-one env {:entity/id player-id} :entity/location))

(defn get-room
  [env room-id]
  (world/query-one env {:room/id room-id} :room/room))

(defn teleport
  [env entity-id location]
  (process
   env
   (command/teleport entity-id location)))

(defn mutate-entity
  [env entity-id mutation]
  (process
   env
   (command/mutate-entity entity-id mutation)))

(defn execute-command
  [env command]
  (process env command))

(defn execute-commands
  [env commands]
  {:pre (action/is-commands? commands)}
  (reduce execute-command env (action/get-commands commands)))

(defn view!
  [{:keys [description]}]
  (prn description))

(defn tick
  [env actions]
  (let [actions-then-advance-initiative (conj actions world/advance-initiative-command)]
    (reduce process env actions-then-advance-initiative)))