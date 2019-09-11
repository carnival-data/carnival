# Frequency Matching

Find a cohort that matches by category frequency an initial cohort.

Input:
A dataset of discrete features F<sub>n</sub>, where the value of each feature is a member of a set F<sub>n</sub> in [A<sub>n</sub>, B<sub>n</sub>, C<sub>n</sub>,...].  Let size(F<sub>n</sub>) be the number of distinct values F<sub>n</sub> can take.

Note that the number of distinct rows is:
sum(i in 1..n) size(F_i)

We want to choose x rows, and know the necessary distribution for each feature.




# Frequency Matcher

## Input
A dataset with rows of discrete features F<sub>n</sub>, where the value of each feature is a member of a set F<sub>n</sub> in [a<sub>n</sub>, b<sub>n</sub>, c<sub>n</sub>...].  Let size(F<sub>n</sub>) be the number of distinct values F<sub>n</sub> can take.

Let a row signature represent the unique values a row can take.  Let the row signature for a row where a<sub>1</sub>, b<sub>2</sub>, a<sub>3</sub> be represented as S<sub>aba</sub>.

Note that the number of distinct row signatures is:

sum(i in 1..n) size(F<sub>i</sub>)


## Goal
We want to choose x rows, and we know the target distribution for each feature.  For example, we want to choose 4 rows out of 10, where 2 rows have an F<sub>1</sub> value of a<sub>1</sub> and 2 rows have an F<sub>1</sub> value of a<sub>a</sub>, and 3 rows have an F<sub>2</sub> value of a<sub>2</sub> and 1 row has an F<sub>2</sub> value of b<sub>2</sub>.  This can be expressed by:

G<sub>1</sub> = [a<sub>1</sub>, a<sub>1</sub>, b<sub>1</sub>, b<sub>1</sub>]

G<sub>2</sub> = [a<sub>2</sub>, a<sub>2</sub>, a<sub>2</sub>, b<sub>2</sub>]


## Algorithm
### Summary
Choose x rows to start.  Let a move be exchanging a selected row for a non-selected row with a different row signature.  Using the currently selected rows, score each possible move by calculating how much it move the distribution towards the goal (aggregate move value), and how many features would be changed by making the move (chaos score).    Continue until the distribution of the selected rows matches the target distribution.

### Steps
1. Randomly choose x rows to start.
2. Calculate the feature distribution of the selected rows and compare it to the goal distribution.
3. Calculate the normalized move value matrix for each feature.

	The columns of the move value matrix are the row signatures of the included rows.  The rows of the move value matrix are the row signatures of row signatures of the excluded rows.  The value of the move value matrix for a particular row and column is the amount it changes the feature distribution in comparison to the goal.



4. Combine move matrices to get an aggregate move value and chaos for each move and use that to rank each possible move.

	- Aggregate move value - For a move S<sub>x</sub> -> S<sub>y</sub>
	- Chaos value - For a move S<sub>x</sub> -> S<sub>y</sub>, the chaos value is the number of non-zero entries

5. Choose the highest ranked move and check to see if the end condition has been reached.  If not, goto 2. 

### Step visualization
histogram of each feature for the selected rows overlayed over the desired histogram









## Example

### Initial State

#### Input Feature Data
Row | F<sub>1</sub> | F<sub>2</sub> 
-------------- | ------------- |-------
1 | a<sub>1</sub> | a<sub>2</sub> 
2 | a<sub>1</sub> | a<sub>2</sub> 
3 | b<sub>1</sub> | b<sub>2</sub> 
4 | c<sub>1</sub> | b<sub>2</sub> 
5 | a<sub>1</sub> | b<sub>2</sub> 

F<sub>1</sub> is set [a<sub>1</sub>, b<sub>1</sub>, c<sub>1</sub>] 

F<sub>2</sub> is set [a<sub>2</sub>, b<sub>2</sub>] 

#### Discrete Rows Signatures

Row Sig | F<sub>1</sub> | F<sub>2</sub> | Rows
-------------- | ------------- |------- | -----
S<sub>aa</sub> | a<sub>1</sub> | a<sub>2</sub> | [1,2]
S<sub>bb</sub> | b<sub>1</sub> | b<sub>2</sub> | [3]
S<sub>cb</sub> | c<sub>1</sub> | b<sub>2</sub> | [4]
S<sub>ab</sub> | a<sub>1</sub> | b<sub>2</sub> | [5]

#### Goal

Feature | Selected Values
------ | -------
F<sub>1</sub>  | [a<sub>1</sub>, b<sub>1</sub>, c<sub>1</sub> ]
F<sub>2</sub>  | [a<sub>2</sub>, b<sub>2</sub>, b<sub>2</sub> ]

### STEP 1 - Random Selection

Randomly select to include a number of rows.  

R<sub>inc</sub> = [1,3,5]

R<sub>exc</sub> = [2, 4]

S<sub>inc</sub> = [S<sub>aa</sub>, S<sub>bb</sub>, S<sub>ab</sub>]

S<sub>exc</sub> = [S<sub>aa</sub>, S<sub>cb</sub>]

Feature | Selected Values
------ | -------
F<sub>1</sub>  | [a<sub>1</sub>, b<sub>1</sub>, a<sub>1</sub>]
F<sub>2</sub>  | [a<sub>2</sub>, b<sub>2</sub>, b<sub>2</sub>]

We have selected rows 1, 3, and 5 as included in our proposed result set.  Given the selection of rows 1, 3, and 5, we have selected for inclusion row signatures S<sub>aa</sub>, S<sub>bb</sub>, and S<sub>ab</sub>.  Conversely, we have selected for exclusion signatures S<sub>aa</sub> and S<sub>cb</sub>.



### STEP 2 - Compute the Move Value Matrices

#### F<sub>1</sub> 
0\1 | S<sub>aa</sub> | S<sub>bb</sub> | S<sub>ab</sub>
--- | --- | --- | ---
S<sub>aa</sub> | 0 | -2 | 0
S<sub>ca</sub> | 2 | 0 | -2

#### F<sub>2</sub>
0\1 | S<sub>aa</sub> | S<sub>bb</sub> | S<sub>ab</sub>
--- | --- | --- | ---
S<sub>aa</sub> | 0 | -2 | -2
S<sub>ca</sub> | 0 | -2 | -2

The move value matrices enumerate the value of making a move, which is the inclusion of a row that is currently excluded and the exclusion of a row that is current included.  

For example, row 1 is currently included while row 2 is excluded.  One possible move is to swap rows 1 and 2, so that row 2 would be included and row 1 would be excluded.  Both rows 1 and 2 have row signagure S<sub>aa</sub>.  The move value matrices above show that V(F<sub>1</sub>,1,2) = V(F<sub>1</sub>,S<sub>aa</sub>,S<sub>aa</sub>) = 0 and V(F<sub>2</sub>,1,2) = V(F<sub>2</sub>,S<sub>aa</sub>,S<sub>aa</sub>) = 0.

We see that V(F<sub>1</sub>,S<sub>ca</sub>,S<sub>aa</sub>) = 2.  This is because such a move would include a row with c<sub>1</sub> and exclude a row with a<sub>1</sub>.  Given that our curent state for F<sub>1</sub> is [a<sub>1</sub>,b<sub>1</sub>,a<sub>1</sub>] and the move would bring it to [c<sub>1</sub>, b<sub>1</sub>, a<sub>1</sub>], we have made two positive changes, excluding an a<sub>1</sub> and including a c<sub>1</sub> resulting in a move value of 2.




