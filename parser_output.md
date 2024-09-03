
Original grammar input:

S -> T
T -> R
T -> a T c
R -> ε
R -> b R

Grammar after Augmentation: 

S' -> . S
S -> . T
T -> . R
T -> . a T c
R -> . ε
R -> . b R

States Generated: 

State = I0
S' -> . S
S -> . T
T -> . R
T -> . a T c
R -> . ε
R -> . b R

State = I1
S' -> S .

State = I2
S -> T .

State = I3
T -> R .

State = I4
T -> a . T c
T -> . R
T -> . a T c
R -> . ε
R -> . b R

State = I5
R -> ε .

State = I6
R -> b . R
R -> . ε
R -> . b R

State = I7
T -> a T . c

State = I8
R -> b R .

State = I9
T -> a T c .


## SLR(1) parsing table

| State | a | c | b | $ | S | T | R |
|---|---|---|---|---|---|---|---|
| I0 | S4 | - | S6 | - | 1 | 2 | 3 |
| I1 | - | - | - | - | - | - | - |
| I2 | - | - | - | - | - | - | - |
| I3 | - | - | - | - | - | - | - |
| I4 | S4 | - | S6 | - | - | 7 | 3 |
| I5 | - | - | - | - | - | - | - |
| I6 | - | - | S6 | - | - | - | 8 |
| I7 | - | S9 | - | - | - | - | - |
| I8 | - | - | - | - | - | - | - |
| I9 | - | - | - | - | - | - | - |

Result of GOTO computation:

GOTO ( I0 , S ) = I1
GOTO ( I0 , T ) = I2
GOTO ( I0 , R ) = I3
GOTO ( I0 , a ) = I4
GOTO ( I0 , ε ) = I5
GOTO ( I0 , b ) = I6
GOTO ( I4 , T ) = I7
GOTO ( I4 , R ) = I3
GOTO ( I4 , a ) = I4
GOTO ( I4 , ε ) = I5
GOTO ( I4 , b ) = I6
GOTO ( I6 , R ) = I8
GOTO ( I6 , ε ) = I5
GOTO ( I6 , b ) = I6
GOTO ( I7 , c ) = I9
