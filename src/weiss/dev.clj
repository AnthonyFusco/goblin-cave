(ns weiss.dev
  (:require
   [engine.action :as action]
   [engine.rule :as rule]
   [engine.action.command :as command]
   [com.wsscode.pathom3.interface.eql :as p.eql]
   [com.wsscode.pathom3.interface.smart-map :as psm]
   [engine.object :as object]
   [engine.core :as core]
   [engine.entity :as entity]
   [engine.world :as world]
   [engine.room :as room]))

(def entitylist [entity/player entity/other])
(map #(select-keys % [:entity/id]) entitylist)
(map :entity/id entitylist)
(:world core/env)
(core/teleport core/env 0 777)

(p.eql/process
 core/env
 '[(:world/entities {:entity/location 0})])

(p.eql/process
 core/env
 [`(engine.entity/update-entity-location
    ~{:entity/id 1
      :entity/location 777})])

(p.eql/process-one
 core/env
 `(engine.entity/update-entity-location
   ~{:entity/id 1
     :entity/location 777}))

(world/query-one core/env {:entity/location 0} :room/room)

(def player (psm/smart-map core/env {:entity/id 0}))
(:entity/location player)
(:room/room player)

(command/teleport 0 1)
(def tmp (core/process core/env (command/teleport 0 1)))
(core/get-location core/env 0)
(core/get-location tmp 0)
(def tmp3
  (core/tick core/env
             [(command/teleport 0 7)
              (command/teleport 0 666)]))
(core/get-location tmp 0)
(first (core/get-history tmp3))

(world/query-one core/env {:entity/id 0} :engine.view/view)

(world/query-one core/env :world/acting)
(world/query-one tmp3 :world/acting)
(world/query-one core/env :world/initiatives)
(world/query-one tmp3 :world/initiatives)

; Rules

(rule/compute-effects object/object-rules :action/eat {} "runtime generated object 1" {})

(rule/compute-effects object/object-rules :action/activate {}
                      :object/lever {:object/id "toto"
                                      :entity/state {:switched? true
                                                      :action/target 0}})

(rule/compute-effects object/object-rules :action/activate {}
                      :object/lever {:object/id "tata"
                                      :entity/state {:switched? false
                                                      :action/target "toto"
                                                      :action/prevent-default? true}})

(rule/compute-entity-effects (assoc object/object-rules :object/special {:semantics #{:semantics/activable}})
                             :action/activate {} object/special-object)

(rule/compute-effects object/object-rules :action/open {} :object/wooden-door {})

(rule/compute-effects object/object-rules :action/open {} :object/iron-door {})

(rule/compute-effects object/object-rules :action/burn {:color "blue"} :object/wooden-door {})

(rule/compute-effects object/object-rules :action/burn {} :object/wooden-door {})

(rule/compute-effects object/object-rules :action/burn {} :object/iron-door {})

(rule/compute-effects object/object-rules :action/open {} :object/door {:entity/state {:name "Titouan la porte"}})
(rule/compute-effects object/object-rules :action/open {} :object/door {})
(rule/compute-effects object/object-rules :action/close {} :object/wooden-door {:state :close})
(rule/compute-effects object/object-rules :action/close {} :object/door {:state :close})
(rule/compute-effects object/object-rules
                      :action/activate {}
                      :object/door {:object/id "some door"
                                     :entity/state {:open? true}})

(rule/compute-entity-effects object/object-rules :action/activate {} object/switched-off-lever-of-healing)

(rule/compute-effects object/object-rules
                      :action/activate {}
                      :object/door {:object/id "some door"
                                     :entity/state {:open? true}})
(rule/compute-effects action/action-rules
                      :action/advance {}
                      :action/default {:id "toto" :action/target 2})

(world/query-one core/env {:entity/id 6} :entity)

; TODO door that overrides activate to activate something else

(rule/execute-effect! core/env object/object-rules
                 {:action/type :action/activate
                  :action/args {:action/target {:entity/id "wooden-door"}}})

(rule/execute-effect! core/env object/object-rules
                 {:action/type :action/activate
                  :action/args {:action/target {:entity/id "wooden-door"
                                                  :entity/rule-type :object/wooden-door
                                                  :entity/state {:exit-to 1
                                                                  :open? false}}}})

(rule/execute-effect! core/env object/object-rules
                 {:action/type :action/activate
                  :action/args {:action/target {:entity/id "lever-of-healing"}}})

; todo door noise side effect of door activation
(rule/execute-effects! core/env object/object-rules
                 (action/make-effects
                  [{:action/type :mutation,
                    :action/args {:switched? true, :meta "self-mutation => lever switched state"}}
                   {:action/type :show, :action/args {:description "You heard a click somewhere"}}
                   {:action/type :action/activate,
                    :action/args {:action/target {:entity/id 0}, :meta "default activate action"}}
                   {:action/type :action/activate,
                    :action/args {:action/target {:entity/id "wooden-door"}, :meta "default activate action"}}
                   {:action/type :action/heal, :action/args {:target :self}}]))

{
 :a.b/c 1
}
; Action

(def advance-obj
  {:action/type :action/advance
   :action/args {:action/target 1}})
;; (action/action-map advance-obj)

{:action/type :action/activate
 :action/args {:action/target 0}}
{:action/type :mutation
 :action/args {:switched? true}}

