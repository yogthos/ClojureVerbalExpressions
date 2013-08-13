(ns verbal-expressions.core
  (:refer-clojure :exclude [compile find replace range or])
  (:require
   [clojure.string :as s]
   [clojure.walk :refer [postwalk]]))

(defrecord VerbalExpression [source modifier prefix suffix pattern])

;; The VE should start matching on all lines.
(def VerEx (VerbalExpression. "" "m" "" "" #"(?m)"))

(defn replace [{regex :pattern} string replacement]
  (s/replace string regex replacement))

(defn regex [{regex :pattern}]
  regex)

(defn source [{source :source}]
  source)

(defn match [{regex :pattern} string]
  (if (nil? (re-find regex string))
    false
    true))

(defn sanitize [string]
  (s/replace string #"([.$*+?^()\[\]{}\\|])" "\\\\$1"))

(defn add [{:keys [prefix source suffix modifier] :as v} value]
  ;; Debuging proposes
  ;;(println (str "(?" modifier ")" prefix source value suffix))
  (assoc v
         :pattern (re-pattern (str "(?" modifier ")" prefix source value suffix))
         :source (str source value)))

(defn anything [verex]
  (add verex "(?:.*)"))

(defn anything-but [verex value]
  (add verex (str "(?:[^" (sanitize value) "]*)")))

(defn something [v value]
  (add v "(?:.+)"))

(defn something-but [v value]
  (add v (str "(?:[^" (sanitize value) "]+)")))

(defn end-of-line [{suffix :suffix :as verex}]
  (add (assoc-in verex [:suffix] (str suffix "$")) ""))

(defn maybe [verex value]
  (add verex (str "(?:" (sanitize value) ")?")))

(defn start-of-line [{prefix :prefix :as verex}]
  (add (assoc-in verex [:prefix] (str "^" prefix)) ""))

(defn find [verex value]
  (add verex (str "(?:" (sanitize value) ")")))

(def then find)

(defn any [verex value]
  (add verex (str "[" (sanitize value) "]")))

(def any-of any)

(defn line-break [verex]
  (add verex "(?:(?:\\n)|(?:\\r\\n))"))

(def br line-break)

(defn range [verex & args]
  (let [from-tos (partition 2 (for [i args] (sanitize i)))]
    (add verex (str "([" (s/join "" (for [i from-tos] (s/join "-" i))) "])"))))

(defn tab [verex]
  (add verex "\t"))

(defn word [verex]
  (add verex "\\w+"))

(defn or
  ([{:keys [prefix suffix] :as v}]
   (-> (assoc v :prefix (str prefix "(?:") :suffix (str ")" suffix))
       (add ")|(?:")))

  ([v value]
   (then (or v) value)))

(defn add-modifier [{modifier :modifier :as v}  m]
  (-> (assoc v :modifier (str m modifier))
      (add "")))

(defn remove-modifier [{modifier :modifier :as v} m]
  (-> (assoc v :modifier (s/replace modifier m ""))
      (add "")))

(defn multiple [v value]
  (let [value (sanitize value)]
    (add v (case (last value) (\* \+) value (str value "+")))))

(defn with-any-case
  ([v]
     (with-any-case v true))
  ([v b]
     (if b (add-modifier v "i") (remove-modifier v "i"))))

(defn search-one-line
  ([v]
     (search-one-line v true))
  ([v b]
     ;; As the VE does matches on all lines, we need to remove the
     ;; modifier when we want to select on one line.
     (if b (remove-modifier v "m") (add-modifier v "m"))))

(defn begin-capture [{suffix :suffix :as v}]
  (-> (assoc v :suffix (str suffix ")"))
      (add "(")))

(defn end-capture [{suffix :suffix :as v}]
  (-> (assoc v :suffix (subs suffix 0 (dec (count suffix))))
      (add ")")))

(defmulti apply-tag (fn [_ [tag]] tag))

(defmethod apply-tag :maybe [regex [_ & args]]
  (apply maybe regex args))
(defmethod apply-tag :anything [regex [_ & args]]
  (apply anything regex args))
(defmethod apply-tag :anything-but [regex [_ & args]]  
  (apply anything-but regex args))
(defmethod apply-tag :something [regex [_ & args]]  
  (apply something regex args))
(defmethod apply-tag :something-but [regex [_ & args]]  
  (apply something-but regex args))
(defmethod apply-tag :start-of-line [regex [_ & args]]  
  (apply start-of-line regex args))
(defmethod apply-tag :end-of-line [regex [_ & args]]
  (apply end-of-line regex args))
(defmethod apply-tag :search-one-line [regex [_ & args]]
  (apply search-one-line regex args))
(defmethod apply-tag :begin-capture [regex [_ & args]]
  (apply begin-capture regex args))
(defmethod apply-tag :end-capture [regex [_ & args]]
  (apply end-capture regex args))
(defmethod apply-tag :with-any-case [regex [_ & args]]
  (apply with-any-case regex args))
(defmethod apply-tag :multiple [regex [_ & args]]
  (apply multiple regex args))
(defmethod apply-tag :remove-modifier [regex [_ & args]]
  (apply remove-modifier regex args))
(defmethod apply-tag :add-modifier [regex [_ & args]]
  (apply add-modifier regex args))
(defmethod apply-tag :add [regex [_ & args]]
  (apply add regex args))
(defmethod apply-tag :or [regex [_ & args]]
  (apply or regex args))
(defmethod apply-tag :word [regex [_ & args]]
  (apply word regex args))
(defmethod apply-tag :tab [regex [_ & args]]
  (apply tab regex args))
(defmethod apply-tag :range [regex [_ & args]]
  (apply range regex args))
(defmethod apply-tag :br [regex [_ & args]]
  (apply br regex args))
(defmethod apply-tag :line-break [regex [_ & args]]
  (apply line-break regex args))
(defmethod apply-tag :any-of [regex [_ & args]]
  (apply any-of regex args))
(defmethod apply-tag :any [regex [_ & args]]
  (apply any regex args))
(defmethod apply-tag :then [regex [_ & args]]
  (apply then regex args))
(defmethod apply-tag :find [regex [_ & args]]
  (apply find regex args))
(defmethod apply-tag :match [regex [_ & args]]
  (apply match regex args))
(defmethod apply-tag :source [regex [_ & args]]  
  (apply source regex args))
(defmethod apply-tag :default [_ [tag & args]]
  (throw (ex-info (str "unrecognized expression: " tag) {:tag tag :args args})))

(defn compile
  ([expressions]
   (compile VerEx expressions))
  ([exp expressions]
   (reduce
    (fn [regex exp]
      (apply-tag regex exp))
    exp
    expressions)))

