(ns engine.rule
  (:require
   [engine.utils :as utils]
   [engine.object :as object]
   [engine.semantic :as semantic]
   [clojure.set :as set]
   [engine.action :as action]))

(defn default-failed-action
  "Returns a default message for failed actions.
   Returns: map with :describe key"
  []
  {:describe "Nothing happens"})

(defn mutation-handler
  "Processes a mutation action. TODO: implement.

   Args:

     - mutation: mutation action map"
  [mutation])

(defn filter-rules
  [xs target-set action-key]
  (let [match-rule-keys (fn [rule]
                          (or (= (key rule) target-set)
                              (contains? target-set (key rule))))
        match-action (fn [rule]
                       (not (nil? (get (val rule) action-key))))] ; (val rule) -> (get-semantic-rule-actions rule)
    (filter #(and (match-action %) (match-rule-keys %)) (seq xs))))

(defn make-handler-args
  "Creates argument map for action handlers.
   Args:
     rule-key: keyword of the rule being applied
     rule: the rule map
     instance: the object instance
     action-params: user-supplied action parameters
   Returns: map with rule-key, rule, name, instance, and action-params"
  [rule-key rule instance action-params]
  (let [state (:engine.action/state instance)
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
  "Performs an action on an object using rule and semantic handlers.

   Args:

     - rules: map of object rules
     - action: keyword for the action to perform
     - action-params: map of action-specific parameters
     - rule-key: keyword identifying the object rule
     - instance: object instance map

   Returns: map with action result, potentially including :describe, :engine.action/mutation, etc."
  [rules action action-params rule-key instance]
  (let [rule (rules rule-key)
        rule-handler (get-in rule [:actions action])
        applicable-semantic-rules (filter-rules semantic/semantic-rules
                                                (set/union (:semantics rule)
                                                           (:intrinsic rule))
                                                action)
        selected-semantic-rule-key (choose-rule (keys applicable-semantic-rules))
        selected-semantic-rule (get semantic/semantic-rules selected-semantic-rule-key)
        handler-args (make-handler-args rule-key rule instance action-params)
        semantic-handler (get selected-semantic-rule action)
        semantic-handler-effects (when semantic-handler (semantic-handler handler-args))
        rule-handler-effects (when rule-handler (rule-handler handler-args))]
    (action/make-effect-list
     (if (or semantic-handler-effects rule-handler-effects)
       (conj semantic-handler-effects rule-handler-effects)
       (action/make-show-effect {:describe (str "Nothing happens when you " (name action) " the " (name rule-key))})))))

(defn perform-on-object
  "Convenience wrapper for perform that extracts object key from instance.

   Args:
     rules: map of object rules
     action: keyword for the action to perform
     action-params: map of action-specific parameters
     instance: object instance map with ::object/object key

   Returns: action result from perform"
  [rules action action-params {::object/keys [object] :as instance}]
  (compute-effects rules action action-params object instance))
