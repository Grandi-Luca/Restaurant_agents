// Agent reception in project 

/* Initial beliefs and rules */
table_available(3).
price(_Service,X) :- .random(R) & X = (10*R)+30.

/* Initial goals */

/* Plans */

// Handle request for table
+request_table[source(A)]
    <-  -request_table[source(A)];
        !check_table(A).

// Table available
+!check_table(A)
    :   table_available(N) & N > 0
    <-  -table_available(N);
        +table_available(N-1);
        .send(A,tell,confirm_table).

// Table not available
-!check_table(A)
    <-  .print("Table not available for customer ",A);
        +waiting_list(A);
        .send(A,tell,refuse_table).

// Send bill to the customer
+bill(Order)[source(A)]
    :   price(Order,X)
    <-  .send(A,tell,receive_bill(X));
        !pop_waiting_list.

// Assign table to one customer from the waiting list
+!pop_waiting_list
    :   waiting_list(X)
    <-  .print("Assigning table to customer ",X);
        .send(X,tell,confirm_table);
        -waiting_list(X).

-!pop_waiting_list.

+customer_leaving[source(A)]
    :   waiting_list(X) & X=A
    <-  -waiting_list(A).
