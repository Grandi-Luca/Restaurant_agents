// Agent reception in project 

/* Initial beliefs and rules */
table_available(3).
price(_Service,X) :- .random(R) & X = (10*R)+100.

/* Initial goals */

/* Plans */

// Table available
+request_table[source(A)]
    :   table_available(N) & N <= 0
    <-  .send(A,tell,refuse_table);
        waiting_list(A).

// Table not available
+request_table[source(A)]
    :   table_available(N) & N > 0
    <-  .send(A,tell,confirm_table);
        -table_avilable(N);
        +table_avilable(N-1).

// Send bill to the customer
+bill(Order)[source(A)]
    :   price(Order,X)
    <-  .send(A,tell,inform_bill(X));
        !pop_waiting_list.

// Assign table to one customer from the waiting list
+!pop_waiting_list
    :   waiting_list(X)
    <-  .send(X,tell,confirm_table);
        -waiting_list(X).

-!pop_waiting_list
    <-  .print("waiting list empty").


+customer_leaving[source(A)]
    :   waiting_list(X) & X=A
    <-  -waiting_list(A).
