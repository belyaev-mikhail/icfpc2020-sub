grammar Language;

Colon
    : ':'
    ;

Equality
    : '='
    ;

fragment Digit
    : [0-9]
    ;

fragment Sym
    : 'a'..'z'
    | 'A' .. 'Z'
    ;


Name:
    Sym (Digit | Sym)*;

Numeral
    : '0'
    | [1-9] Digit*
    ;
NegNumeral
    : '-' Numeral
    ;

identifier
    : Colon Numeral
    ;

fileContent
    : statements EOF
    ;

statements
    : statement*
    ;


statement
    : identifier Equality expression
    | Name Equality expression
    ;

number
    : Numeral
    | NegNumeral
    ;


expression
    : identifier
    | number
    | nil
    | function
    | ap expression expression
    | bools
    ;

function
    : list_rule
    | neg_rule
    | inc_rule
    | dec_rule
    | add_rule
    | mul_rule
    | div_rule
    | eq_rule
    | lt_rule
    | dem_rule
    | mod_rule
    | combinator
    | isnil_rule

    ;

ap
    : 'ap'
    ;

cons_rule
    : 'cons'
    ;

nil
    : 'nil'
    ;

neg_rule: 'neg' ;

inc_rule: 'inc';
dec_rule: 'dec';
add_rule: 'add';
mul_rule: 'mul';
div_rule: 'div';
eq_rule: 'eq';
lt_rule : 'lt';

dem_rule :'dem';
mod_rule : 'mod';
car_rule: 'car';
cdr_rule:'cdr';

send_rule: 'send';
isnil_rule: 'isnil';

list_rule
    : cons_rule
    | car_rule
    | cdr_rule
    ;
combinator
    : s_comb_rule
    | b_comb_rule
    | c_comb_rule
    | i_comb_rule
    ;

bools
    : true_rule
    | false_rule
    ;

true_rule: 't';
false_rule:'f';

s_comb_rule: 's';
b_comb_rule: 'b';
c_comb_rule: 'c';
i_comb_rule: 'i';

WS  :  [ \t\r\n]+ -> channel(HIDDEN)
    ;
