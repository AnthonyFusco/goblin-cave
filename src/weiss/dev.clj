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

(rule/apply-action core/env
                   object/object-rules
                   (action/make-action :action/eat)
                   {:entity/id "boulgiboulga"})

; (:world (with-entities env [{:entity/id "toot" :entity/name "toto"}]))

(rule/apply-action (core/with-entities core/env
                     [{:entity/id "toto"
                       :entity/rule-type :object/lever
                       :entity/state {:switched? true
                                      :action/target 0}}])
                   (action/make-action :action/activate)
                   {:entity/id "toto"})

(rule/apply-action (core/with-entities core/env
                     [{:entity/id "toto"
                       :entity/rule-type :object/lever
                       :entity/state {:switched? false
                                      :action/target "toto"
                                      :action/prevent-default? true}}])
                   (action/make-action :action/activate)
                   {:entity/id "toto"})

(rule/apply-action core/env
                   (assoc object/object-rules :object/special {:semantics #{:semantic/activable}})
                   (action/make-action :action/activate)
                   {:entity/id "special-object"})

(rule/apply-action core/env
                   (action/make-action :action/open)
                   {:entity/id "iron-door"})

(rule/apply-action core/env
                   (action/make-action :action/burn {:color "blue"})
                   {:entity/id "wooden-door"})

(rule/apply-action core/env
                   (action/make-action :action/burn)
                   {:entity/id "iron-door"})

(rule/apply-action core/env
                   (action/make-action :action/activate)
                   {:entity/id "wooden-door"})

(rule/apply-action core/env
                   (action/make-action :action/activate)
                   {:entity/id "lever-of-healing"})

; wrong
(rule/apply-action core/env
                   action/action-rules
                   (action/make-action :action/advance)
                   {:entity/id 0 :action/target 2})

(world/query-one core/env {:entity/id 0} :entity)

; TODO door that overrides activate to activate something else

(rule/reify-effect core/env object/object-rules
                   {:action/type :action/activate
                    :action/args {:action/target {:entity/id "wooden-door"}}})

(rule/reify-effect core/env object/object-rules
                   {:action/type :action/activate
                    :action/args {:action/target {:entity/id "lever-of-healing"}
                                  :toto 1}})

; TODO door noise side effect of door activation
(rule/reify-effects core/env object/object-rules
                    (action/make-effects
                     [{:action/type :action/mutation,
                       :action/target {:entity/id "lever-of-healing"}
                       :action/mutation {:switched? true}}
                      {:action/type :show, :action/args {:description "You heard a click somewhere"}}
                      {:action/type :action/activate,
                       :action/args {:action/target {:entity/id 0}}}
                      {:action/type :action/activate,
                       :action/args {:action/target {:entity/id "wooden-door"}}}
                      {:action/type :action/heal, :action/args {:target :self}}]))

(rule/reify-effects core/env object/object-rules
                    (action/make-effects
                     [{:action/type :action/activate,
                       :action/args {:action/target {:entity/id "wooden-door"}}}]))

(rule/reify-effects core/env object/object-rules
                    (action/make-effects
                     [{:action/type :action/mutation
                       :action/target {:entity/id "wooden-door"}
                       :action/mutation {:open? true}}]))

(world/query-one core/env {:entity/id "wooden-door"} :entity)
(world/query-one core/env :world/entities)
(world/query-entity core/env "wooden-door")

(def effects
  (rule/reify-effects core/env object/object-rules
                      (rule/apply-action
                       core/env
                       (action/make-action :action/activate)
                       object/switched-off-lever-of-healing)))

(def after (core/mutate-entity
            core/env "wooden-door"
            {:open? true}))

(count (:action/commands effects))

(core/process core/env (first (:action/commands effects)))

(reduce #(core/process %1 %2)
        core/env (:action/commands effects))

(reduce core/process core/env (action/get-commands effects))

(def after2 (core/execute-commands core/env effects))
(core/get-history after2)
(world/query-entity core/env "wooden-door")
(world/query-entity after2 "wooden-door")
(world/query-entity core/env "lever-of-healing")
(world/query-entity after2 "lever-of-healing")


; A turn
(def my-action-effects
  (rule/apply-action
   core/env
   (action/make-action :action/activate)
   object/switched-off-lever-of-healing))


(def my-reified-effects
  (rule/reify-effects core/env object/object-rules
                      my-action-effects))

(def my-new-env
  (core/execute-commands
   core/env
   my-reified-effects))

(world/query-entity core/env "wooden-door")
(world/query-entity my-new-env "wooden-door")
(world/get-log core/env)
(world/get-log my-new-env)
; done
