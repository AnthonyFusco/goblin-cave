(ns engine.room
  (:require [engine.utils :as utils]
            [engine.object :as object]
            [com.wsscode.pathom3.connect.operation :as pco]
            [com.wsscode.pathom3.connect.built-in.resolvers :as pbir]
            [engine.entity :as entity]
            [clojure.string :as str]))

(def room1 {:room/id 0
            :room/desc {:room/text "a small room"}})
(def room2 {:room/id 1
            :room/desc {:room/text "a big hall"}})
(def room3 {:room/id 2
            :room/desc {:room/text "a strange chamber"}
            :room/exits [{:exit/cardinal :exit.cardinal/west
                          :exit/type :exit.type/secret-door
                          :exit/state :exit.state/hidden
                          :room/next 1}]})

(def rooms (into {}
                 (map (juxt :room/id identity)
                      [room1 room2 room3])))

(def room-id-equivalence
  (pbir/equivalence-resolver :entity/location :room/id))

(pco/defresolver room-resolver
  [{:keys [world room/id]}]
  {:room/room (do (prn "room resolver")
                  (get (:world/dungeon world) id))})

(pco/defresolver room-description-resolver
  [{:keys [room/room entities-in-location]}]
  {:room/room-description
   (let [room-desc (:room/desc room)
         room-text (:room/text room-desc)
         entities-description (map entity/get-description entities-in-location)]
     (prn "room description resolver")
     {:entities entities-in-location
      :room room
      :description (str "in: " room-text " you see: " (str/join ", " entities-description))})})

(defn update-room
  [world room]
  (assoc-in world [:room/dungeon (:room/id room)] room))

(defn mutate-room-fn
  [room args]
  (merge room args))

(pco/defmutation mutate-room
  [{:keys [world room/room args]}]
  (let [updated-room (mutate-room-fn room args)
        updated-world (update-room world updated-room)]
    (utils/computation-valid updated-world)))

(def resolvers [room-resolver room-id-equivalence room-description-resolver])
