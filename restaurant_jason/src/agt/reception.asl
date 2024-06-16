// Agent reception in project 

/* Initial beliefs and rules */
max_table_available(3).
price(_Service,X) :- .random(R) & X = (10*R)+30.

/* Initial goals */

/* Plans */

// Handle request for table
+request_table[source(A)]
    <-  -request_table[source(A)];
        !check_table(A).

// Table available
+!check_table(A)
    :   max_table_available(N) & .count(table(_), TO) & N > TO
    <-  +table(A);
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
        !pop_waiting_list(A).

// Assign table to one customer from the waiting list
+!pop_waiting_list(A)
    :   waiting_list(X) & table(A)
    <-  .send(X,tell,confirm_table);
        +table(X);
        -table(A);
        .print("Assigning table to customer ",X);
        -waiting_list(X).

-!pop_waiting_list(A).

+customer_leaving[source(A)]
    <-  -customer_leaving[source(A)];
        !handle_waiting_customer_leaving(A).

+!handle_waiting_customer_leaving(A)
    :   waiting_list(X) & X=A
    <-  -waiting_list(A).
