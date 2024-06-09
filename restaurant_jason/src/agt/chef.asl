// Agent chef in project 

/* Initial beliefs and rules */
random_int(X,Y,Z) :- .random(R) & Z = math.round((R*Y)+X).

/* Initial goals */
!register.

/* Plans */

// Normalize random number between X and Y
+!normalize_random(R, Y, Z)
    :   R >= Y
    <-  Z = Y - 1.
+!normalize_random(R, Y, Z)
    :   R < Y
    <-  Z = R.

// Register chef in DF
+!register 
    <-  .df_register("chef").

// Response to a new request for an order
+request_order(Order, Customer)[source(A)]
    : random_int(1, 10, Z)
    <-  +order_in_progress(Order, Customer);
        .print("Order in progress: ", Order, ". Customer: ", Customer, ". Time: ", Z, " seconds");
        .wait(Z*1000);
        !order_ready(Order, Customer).

// Order is ready
+!order_ready(Order, Customer) 
    <-  .print("Order ready: ", Order);
        !search_waiter(Waiter);
        .send(Waiter, tell, order_ready);
        +interlocutor(Waiter, Order, Customer).

// Chef receives a propose to serve an order from waiter
+waiter_propose[source(W)]
    :   interlocutor(Waiter, O, C) & Waiter = W
    <-  -waiter_propose[source(W)];
        -interlocutor(W, O, C);
        -order_in_progress(O, C);
        .send(W, tell, serve_order(O, C)).

-waiter_propose[source(W)].

// Chef receives a refuse to serve an order from waiter
+waiter_refuse[source(W)]
    :   interlocutor(Waiter, O, C) & Waiter = W
    <-  -waiter_refuse[source(W)];
        -interlocutor(W, O, C);
        !order_ready(O, C).

-waiter_refuse[source(W)].
    

// Try to find a waiter
+!search_waiter(W)
    :   .print("Searching for waiter") &
        .df_search("waiter", Ws) &
        .length(Ws) > 0 &
        random_int(0, .length(Ws), P)
    <-  !normalize_random(P, .length(Ws), X);
        !get_waiter(X, 0, Ws, W).

// Failure: no waiter was found wait 4 seconds and try again
-!search_waiter(W)
    <-  .print("No waiter found");
        .wait(4000);
        !search_waiter(W).

// Get a waiter in P position
+!get_waiter(P, I, [HEAD|TAIL], W)
    :   P==I
    <-  W = HEAD.

+!get_waiter(P,I, [HEAD|TAIL], W)
    :   P\==I
    <-  !get_waiter(P, I+1, TAIL, W).