(ns weiss.dev
  (:require
   [engine.action :as action]
   [engine.rule :as rule]
   [clojure.set :as set]
   [com.wsscode.pathom3.interface.eql :as p.eql]
   [com.wsscode.pathom3.interface.smart-map :as psm]
   [engine.object :as object]
   [engine.core :as core]
   [engine.entity :as entity]
   [engine.room :as room]))

(def entitylist [entity/player entity/other])
(map #(select-keys % [::entity/id]) entitylist)
(map ::entity/id entitylist)
(:world core/env)
(core/teleport core/env 0 777)

(p.eql/process
 core/env
 '[(:engine.core/entities {::entity/location 0})])

(p.eql/process
 core/env
 [`(engine.entity/update-entity-location
    ~{::entity/id 1
      ::entity/location 777})])

(p.eql/process-one
 core/env
 `(engine.entity/update-entity-location
   ~{::entity/id 1
     ::entity/location 777}))

(core/query-one core/env {::entity/location 0} ::room/room)

(def player (psm/smart-map core/env {::entity/id 0}))
(::entity/location player)
(::room/room player)

(core/teleport-command 0 1)
(def tmp (core/process core/env (core/teleport-command 0 1)))
(core/get-location core/env 0)
(core/get-location tmp 0)
(def tmp3
  (core/tick core/env
             [(core/teleport-command 0 7)
              (core/teleport-command 0 666)]))
(core/get-location tmp3 0)
(first (core/get-history tmp3))

(core/query-one core/env {::entity/id 0} :engine.view/view)

(core/query-one core/env ::core/acting)
(core/query-one tmp3 ::core/acting)
(core/query-one core/env ::core/initiatives)
(core/query-one tmp3 ::core/initiatives)

(rule/compute-effects object/object-rules ::action/eat {} "runtime generated object 1" {})

(rule/compute-effects object/object-rules ::action/activate {}
                      ::object/lever {::object/id "toto"
                                      ::action/state {:switched? true
                                                            ::action/target 0}})

(rule/compute-effects object/object-rules ::action/activate {}
                      ::object/lever {::object/id "tata"
                                      ::action/state {:switched? false
                                                            ::action/target "toto"
                                                            ::action/prevent-default? true}})

(rule/perform-on-object object/object-rules ::action/activate {} room/switched-off-lever-of-healing)
(rule/perform-on-object (assoc object/object-rules ::object/special {:semantics #{:semantics/activable}})
                        ::action/activate {} room/special-object)

(rule/compute-effects object/object-rules ::action/open {} ::object/wooden-door {})

(rule/compute-effects object/object-rules ::action/open {} ::object/iron-door {})

(rule/compute-effects object/object-rules ::action/burn {:color "blue"} ::object/wooden-door {})

(rule/compute-effects object/object-rules ::action/burn {} ::object/wooden-door {})

(rule/compute-effects object/object-rules ::action/burn {} ::object/iron-door {})

(rule/compute-effects object/object-rules ::action/open {} ::object/door {::action/state {:name "Titouan la porte"}})
(rule/compute-effects object/object-rules ::action/open {} ::object/door {})
(rule/compute-effects object/object-rules ::action/close {} ::object/wooden-door {:state :close})
(rule/compute-effects object/object-rules ::action/close {} ::object/door {:state :close})
(rule/compute-effects object/object-rules ::action/close {} ::object/door {:state :open})
