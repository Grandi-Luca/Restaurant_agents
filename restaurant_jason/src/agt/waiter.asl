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
    :   busy(B) & B = false
    <-  .send(A,tell,waiter_propose);
        +current_customer(A);
        -busy(false);
        +busy(true).

+customer_ready[source(A)]
    :   busy(B) & B = true
    <-  .send(A,tell,waiter_refuse).

// Handle the request from the customer to take the order
+take_order(Order)[source(A)]
    :   current_customer(Ag) & Ag = A
    <-  .send(chef,tell,request_order(Order, A));
        -busy(true);
        +busy(false);
        -current_customer(A).

// Handle the request from the Chef to take the order
+order_ready[source(A)]
    :   provider(A, "chef") &
        busy(B) & B = true
    <-  .send(A,tell,refuse).

+order_ready[source(A)]
    :   provider(A, "chef") &
        busy(B) & B = false
    <-  .send(A,tell,propose).

// Handle the request from the Chef to serve the order
+serve_order(Order, Customer)[source(A)]
    :   provider(A, "chef")
    <-  .send(Customer,tell,recive_order(Order)).