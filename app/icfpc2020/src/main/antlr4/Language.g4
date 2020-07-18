grammar Language;

Colon
    : ':'
    ;

Comma
    : ','
    ;

OpenPar
    : '('
    ;

ClosePar
    : ')'
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
    | list_expression
    | function
    | application
    | bools
    ;

application
    : ap expression expression
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
    | interactions
    | combinator
    | isnil_rule
    | power_of_2_rule
    | if0_rule
    ;

ap  : 'ap'  ;

cons_rule  : 'cons' ;
vec_rule: 'vec';
nil : 'nil' ;
car_rule: 'car';
cdr_rule:'cdr';
isnil_rule: 'isnil';


neg_rule: 'neg' ;
inc_rule: 'inc';
dec_rule: 'dec';
add_rule: 'add';
mul_rule: 'mul';
div_rule: 'div';
eq_rule: 'eq';
lt_rule : 'lt';
power_of_2_rule: 'pwr2';

if0_rule: 'if0';

dem_rule :'dem';
mod_rule : 'mod';
send_rule: 'send';
interact_rule : 'interact';

draw_rule : 'draw';
checkerboard_rule : 'checkerboard';
multipledraw_rule : 'multipledraw';

interactions
    : send_rule
    | mod_rule
    | dem_rule
    | interact_rule
    | draw_rule
    | checkerboard_rule
    | multipledraw_rule
    ;

list_expression
    : empty_list
    | empty_list_with_comma
    | non_empty_list
    ;

empty_list
    : OpenPar ClosePar
    ;

empty_list_with_comma
    : OpenPar Comma ClosePar
    ;

non_empty_list
    : OpenPar expression (Comma expression)* ClosePar
    ;

list_rule
    : cons_rule
    | car_rule
    | cdr_rule
    | vec_rule
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
