# async-rule-evaluator

This module is a Java version of a Javascript package [async-rule-evaluator](https://github.com/gas-buddy/async-rule-evaluator).
A simple DSL based on [Filtrex](https://github.com/joewalnes/filtrex) and its forks. The intent is to add lazy evaluation and
async property lookup support, as the NPM module has, but right now this module is synchronous. So this means you must present
all values you intend to use in the rules up front as a map.

The main thing we took from Filtrex is the grammar, which seemed like a good one,
but modified for our use case. For example, we removed the `x of y` syntax in favor
of lodash path-based lookup (i.e. `x.y`).

Features
--------
*   **Simple!** End user expression language looks like this `transactions <= 5 and abs(profit) > 20.5`
*   **Safe!** You as the developer have control of which data can be accessed and the functions that can be called. Expressions cannot escape the sandbox.
*   **Predictable!** Because users can't define loops or recursive functions, you know you won't be left hanging.

Expressions
-----------

There are only 3 types: numbers, strings and arrays of these. Numbers may be floating point or integers. Boolean logic is applied on the truthy value of values (e.g. any non-zero number is true, any non-empty string is true, otherwise false).

Okay, I lied to you, there are also objects whose properties can be accessed with dot and array notation (thanks to lodash.toPath). And there's undefined. But everything else is just numbers, strings and arrays!

Values | Description
--- | ---
43, -1.234 | Numbers
"hello" | String
" \\" \\\\ " | Escaping of double-quotes and blackslash in string
foo, a.b.c, 'foo-bar' | External data variable defined by application (may be numbers or strings)

Numeric arithmetic | Description
--- | ---
x + y | Add
x - y | Subtract
x * y | Multiply
x / y | Divide
x % y | Modulo
x ^ y | Power

Comparisons | Description
--- | ---
x == y | Equals
x != y | Does not equal
x < y | Less than
x <= y | Less than or equal to
x > y | Greater than
x >= y | Greater than or equal to
x ~= y | Regular expression match
x in (a, b, c) | Equivalent to (x === a or x === b or x === c)
x not in (a, b, c) | Equivalent to (x != a and x != b and x != c)
x in~ (a, b, c) | Equivalent to (String(x) == String(a) or String(x) == String(b) or String(x) == String(c))
x not in~ (a, b, c) | Equivalent to (String(x) != String(a) and String(x) != String(b) and String(x) != String(c))

Boolean logic | Description
--- | ---
x or y | Boolean or
x and y | Boolean and
not x | Boolean not
x ? y : z | If boolean x, value y, else z
( x ) | Explicit operator precedence

Objects and arrays | Description
--- | ---
(a, b, c) | Array
[a, b, c] | Array (synonym)
a in b | Array a is a subset of array b
a in~ b | Array a is a subset of array b using string conversion for comparison
x.y | Property y of object x (x can be a function/promise, y can be a function/promise)

Built-in functions | Description
--- | ---
abs(x) | Absolute value
ceil(x) | Round floating point up
floor(x) | Round floating point down
max(a, b, c...) | Max value (variable length of args)
min(a, b, c...) | Min value (variable length of args)
random() | Random floating point from 0.0 to 1.0
round(x) | Round floating point
length(x) | Return the length of an array (or the length property of an object), or 0 if x is falsy
lower(x) | If x is null or undefined, return it, else return x.toString().toLocaleLowerCase()
sqrt(x) | Square root
substr(x, start, end) | Get a part of a string

Operator precedence follows that of any sane language.