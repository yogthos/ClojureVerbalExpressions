ClojureVerbalExpressions
=======================

## Installation
Add [![Clojars Project](https://img.shields.io/clojars/v/verbal-expressions.svg)](https://clojars.org/verbal-expressions) too your project.clj file.

## Usage

```clojure
(require '[verbal-expressions.core :as verex])

;; Create an example of how to test for correctly formed URLs

(def tester  (-> verex/VerEx
                 (start-of-line)
                 (find "http")
                 (maybe "s")
                 (find "://")
                 (maybe "www.")
                 (anything-but " ")
                 (end-of-line)))

;; Create an example URL
(def test-url "https://www.google.com")

;; Test if the URL is valid
(if (match tester test-url)
  (println "Valid URL"))

;; Print the generated regex
(println (source tester)) 
;; => ^(http)(s)?(\:\/\/)(www\.)?([^\ ]*)$
```

Alternatively, it's possible to use a declarative syntax to build the regex:

```clojure
;; compile regex
(def tester (verex/compile
              [[:start-of-line]
               [:find "http"]
               [:maybe "s"]
               [:find "://"]
               [:maybe "www."]
               [:anything-but " "]
               [:end-of-line]]))

;; Test if the URL is valid
(if (match tester test-url)
  (println "Valid URL"))
```

### Replacing strings
```clojure
;; Create a test string
(def replace-me "Replace bird with a duck")

;; Create an expression that looks for the word "bird"
(def expression (find VerEx "bird"))

;; Execute the expression in VerEx
(def result-verex (replace expression replace-me "duck"))
(println result-verex)
```
### "Shorthand" for string replace
```clojure
;; (def result (clojure.string/replace "We have a red house" #"red" "blue")
(def result (replace (find VerEx "red") "We have a red house" "blue"))
(println result)
```
## Other implementations  
You can view all implementations on [VerbalExpressions.github.io](http://VerbalExpressions.github.io)
