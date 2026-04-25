(ns engine.rule
  (:require
   [clojure.set :as set]
   [engine.action :as action]
   [engine.entity :as entity]
   [engine.object :as object]
   [engine.semantic :as semantic]
   [engine.world :as world]))

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
  [env rule-key rule instance action-params]
  (let [state (:entity/state instance)
        name (or (:name state) (:name rule) (name rule-key))]
    {:env env
     :rule-key rule-key
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

(defn apply-action
  "Returns effect-list"
  ([env action target]
   (apply-action env object/object-rules action target))
  ([env rules action target]
   {:pre [(not (nil? action))
          (not (nil? target))]}
   (let [instance (world/query-one env target :entity) ; TODO decide type according to target key
         rule-type (:entity/rule-type instance)
         rule (rules rule-type)
         action-type (:action/type action)
         action-args (:action/args action)
         rule-handler (get-in rule [:actions action-type])
         applicable-semantic-rules (filter-rules semantic/semantic-rules
                                                 (set/union (:semantics rule)
                                                            (:intrinsic rule))
                                                 action-type)
         selected-semantic-rule-key (choose-rule (keys applicable-semantic-rules))
         selected-semantic-rule (get semantic/semantic-rules selected-semantic-rule-key)
         semantic-handler (get selected-semantic-rule action-type)
         handler-args (make-handler-args env rule-type rule instance action-args)
         semantic-handler-effects (when semantic-handler (semantic-handler handler-args))
         rule-handler-effects (when rule-handler (rule-handler handler-args))]
     (action/make-effects
      (if (or semantic-handler-effects rule-handler-effects)
        (conj semantic-handler-effects rule-handler-effects)
        (action/make-show-effect
         {:describe
          (str "Nothing happens when you " (name action-type) " the " (or (entity/get-name instance)
                                                                          (name rule-type)))}))))))

(defn apply-on-target-effect-handler
  [env rules effect]
  {:pre [(not (nil? (:action/target (:action/args effect))))
         (not (nil? (:action/type effect)))]}
  (apply-action env rules
                (action/make-action (:action/type effect) (:action/args effect))
                (:action/target (:action/args effect))))

(def effect-handlers
  {:action/activate apply-on-target-effect-handler})

(defn reify-effect
  [env rules effect]
  (let [effect-type (:action/type effect)
        handler (effect-handlers effect-type)]
    (if handler
      (handler env rules effect)
      (do
        (prn "No handler for " effect-type)
        (action/make-effects effect)))))

; maybe should loop until there is no differences between loops
(defn reify-effects
  "Take an effect object and returns an effect object"
  [env rules effects]
  {:pre [(contains? effects :action/effects)]}
  (reduce
   (fn [coll effect] (merge-with into coll (reify-effect env rules effect)))
   (action/make-effects)
   (action/get-effect-list effects)))
