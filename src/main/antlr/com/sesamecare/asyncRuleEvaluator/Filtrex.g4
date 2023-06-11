grammar Filtrex;

// Entry Point
expressions: e EOF;

e: '(' e ')'                        # ParenExpression
| '-' e                             # UnaryMinus
| NOT e                             # Not
| NOT_TILDE e                       # AlternativeNot
| e '^' e                           # Power
| e op=('*' | '/') e                # MulDiv
| e '%' e                           # Modulo
| e op=('+' | '-') e                # AddSub
| e '<' e                           # LessThan
| e '<=' e                          # LessThanEquals
| e '>' e                           # GreaterThan
| e '>=' e                          # GreaterThanEquals
| e EQUALS e                        # Equals
| e NOT_EQUALS e                    # NotEquals
| e REGEX_MATCH e                   # RegexMatch
| e IN e                            # In
| e INEXACTIN e                     # InexactIn
| e AND e                           # And
| e OR e                            # Or
| e '?' e ':' e                     # Ternary
| '[' e ']'                         # ArrayExpression
| '(' array ',' e ')'               # ArrayWithCommaExpression
| '[' array ',' e ']'               # ArrayWithCommaBracketExpression
| NUMBER                            # Number
| STRING                            # String
| SYMBOL                            # Symbol
| SYMBOL '(' ')'                    # SymbolFunctionCall
| SYMBOL '(' argsList ')'           # SymbolFunctionCallWithArgs
| e NOT IN e                        # NotIn
| e NOT INEXACTIN e                 # NotInexactIn
;

argsList
: e                                 # SingleArg
| argsList ',' e                    # Args
;

array
: e                                 # SingleElement
| array ',' e                       # ArrayElements
;

// Tokens
AND: 'and';
OR: 'or';
NOT: 'not';
INEXACTIN: 'in~';
IN: 'in';
EQUALS: '==';
NOT_EQUALS: '!=';
REGEX_MATCH: '~=';
NOT_TILDE: '~';
QUESTION: '?';
COLON: ':';
NUMBER: [0-9]+('.'[0-9]+)? ;
STRING: '"' ( '\\"' | '\\\\' | ~["\\])* '"';
SYMBOL: [a-zA-Z$_]([\\.a-zA-Z0-9$_])* | '\'' ( '\\\'' | '\\\\' | ~['\\])* '\'';
WS: [ \t\r\n]+ -> skip ;