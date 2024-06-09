// Agent waiter in project 

/* Initial beliefs and rules */
busy(false).


/* Initial goals */

!register.

/* Plans */

+!register 
    <-  .df_register("waiter");
        .df_subscribe("chef").

+!deregister <- .df_deregister("waiter").

// Handle the request from the customer ready to order
+customer_ready[source(A)]
    <-  -customer_ready[source(A)];
        !check_availability(A).

+!check_availability(A)
    :   busy(B) & B = false
    <-  !deregister;
        -busy(false);
        +busy(true);
        .print("I'm available for the agent ", A);
        .send(A,tell,waiter_propose);
        +current_interlocutor(A).

-!check_availability(A)
    <-  .send(A,tell,waiter_refuse).


// Handle the request from the customer to take the order
+take_order(Order)[source(A)]
    :   current_interlocutor(Ag) & Ag = A
    <-  -take_order(Order)[source(A)];
        .print("Took the order ", Order, " from the customer ", A);
        .send(chef,tell,request_order(Order, A));
        -current_interlocutor(A);
        -busy(true);
        +busy(false);
        !register.

// Handle the request from the Chef to take the order
+order_ready[source(A)]
    <-  -order_ready[source(A)];
        !check_availability(A).


// Handle the request from the Chef to serve the order
+serve_order(Order, Customer)[source(A)]
    :   provider(A, "chef")
    <-  -serve_order(Order, Customer)[source(A)];
        .send(Customer,tell,recive_order(Order));
        -busy(true);
        +busy(false);
        !register.