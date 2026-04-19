(ns engine.rule
  (:require
   [engine.semantic :as semantic]
   [clojure.set :as set]
   [engine.world :as world]
   [engine.action :as action]))

(defn default-failed-action
  "Returns a default message for failed actions.
   Returns: map with :describe key"
  []
  {:describe "Nothing happens"})

(defn filter-rules
  [xs target-set action-key]
  (let [match-rule-keys (fn [rule]
                          (or (= (key rule) target-set)
                              (contains? target-set (key rule))))
        match-action (fn [rule]
                       (not (nil? (get (val rule) action-key))))] ; (val rule) -> (get-semantic-rule-actions rule)
    (filter #(and (match-action %) (match-rule-keys %)) (seq xs))))

(defn make-handler-args
  [rule-key rule instance action-params]
  (let [state (:entity/state instance)
        name (or (:name state) (:name rule) (name rule-key))]
    {:rule-key rule-key
     :rule rule
     :name name
     :instance (or instance {})
     :action-params action-params}))

(defn choose-rule
  "Chooses the most specific rule from a list of applicable rules"
  [applicable-rule-keys]
  (case (count applicable-rule-keys)
    0  nil
    1 (first applicable-rule-keys)
    (->> applicable-rule-keys
         (map #(if (seqable? %)
                 {:count (count %) :value %}
                 {:count 1 :value %}))
         (sort-by :count >)
         first
         :value)))

(defn compute-effects
  "Returns effect-list"
  [rules action action-params rule-key instance]
  {:pre [(not (nil? action))
         (not (nil? rule-key))
         (not (nil? instance))]}
  (let [rule (rules rule-key)
        rule-handler (get-in rule [:actions action])
        applicable-semantic-rules (filter-rules semantic/semantic-rules
                                                (set/union (:semantics rule)
                                                           (:intrinsic rule))
                                                action)
        selected-semantic-rule-key (choose-rule (keys applicable-semantic-rules))
        selected-semantic-rule (get semantic/semantic-rules selected-semantic-rule-key)
        semantic-handler (get selected-semantic-rule action)
        handler-args (make-handler-args rule-key rule instance action-params)
        semantic-handler-effects (when semantic-handler (semantic-handler handler-args))
        rule-handler-effects (when rule-handler (rule-handler handler-args))]
    (action/make-effects
     (if (or semantic-handler-effects rule-handler-effects)
       (conj semantic-handler-effects rule-handler-effects)
       (action/make-show-effect {:describe (str "Nothing happens when you " (name action) " the " (name rule-key))})))))

(defn compute-entity-effects
  "Returns effect-list"
  ([rules effect]
   (let [action-type (:action/type effect)
         instance (:action/args effect)]
     (compute-entity-effects rules action-type {} instance)))
  ([rules action action-params {:keys [entity/rule-type] :as instance}]
   (compute-effects rules action action-params rule-type instance)))

(defn reify-activate-effect!
  [env args]
  (let [{:keys [:action/target]} args
        target-entity (if (nil? (:entity/state target))
                        (world/query-one env target :entity)
                        target)]
    (if (nil? target-entity)
      (throw (ex-info "Target entity not found" {:target target}))
      {:action/type :action/activate
       :action/args target-entity})))

(defn activate-effect-handler!
  [env rules args]
  (let [reified-effect (reify-activate-effect! env args)]
    (compute-entity-effects rules reified-effect)))

(defn execute-effect!
  [env rules effect]
  (let [effect-type (:action/type effect)
        effect-args (:action/args effect)]
    (case effect-type
      :action/activate (activate-effect-handler! env rules effect-args)
      (do
        (prn "No handler for " effect-type)
        (action/make-effects effect)))))

(defn execute-effects!
  "Take an effect object and returns an effect object"
  [env rules effects]
  {:pre [(contains? effects :action/effects)]}
  (reduce
   (fn [coll effect] (merge-with into coll (execute-effect! env rules effect)))
   (action/make-effects)
   (action/get-effect-list effects)))
