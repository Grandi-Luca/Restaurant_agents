// Agent chef in project 

/* Initial beliefs and rules */
random_int(X,Y,Z) :- .random(R) & Z = math.round((R+X)*Y).

/* Initial goals */
!register.

/* Plans */

// Normalize random number between X and Y
+!normalize_random(X, Y, Z)
    :   X >= Y
    <-  Z = Y - 1.
+!normalize_random(X, Y, Z)
    :   X < Y
    <-  Z = X.

// Register chef in DF
+!register 
    <-  .df_register("chef").

// Response to a new request for an order
+request_order(Order, Customer)[source(A)]
    : random_int(1, 10, Z)
    <-  +order_in_progress(Order, Customer);
        .print("Order in progress: ", Order, " Time to prepare: ", Z, " seconds");
        .wait(Z*1000);
        !order_ready(Order, Customer).

// Order is ready
+!order_ready(Order, Customer) 
    <-  .print("Order ready: ", Order);
        !search_waiter(Waiter);
        .send(Waiter, cfp, order_ready);
        +interlocutor(Waiter, O, C).

// Chef receives a propose to serve an order from waiter
+propose[source(W)]
    :   interlocutor(Waiter, O, C) & Waiter = W
    <-  .send(W, tell, serve_order(O, C));
        -interlocutor(Waiter, O, C);
        -order_in_progress(Order, Customer).

// Chef receives a refuse to serve an order from waiter
+refuse[source(W)]
    :   interlocutor(Waiter, O, C) & Waiter = W
    <-  .send(W, tell, refuse_order(O, C));
        -interlocutor(Waiter, O, C);
        !order_ready(O, C).

// Try to find a waiter
+!search_waiter(W)
    :   .print("Searching for waiter") &
        .df_search("waiter", Ws) &
        .length(Ws) > 0 &
        random_int(.length(Ws), P)
    <-  !normalize_random(P, .length(Ws), X);
        !waiter(X, 0, Ws, W).

// Failure: no waiter was found wait 4 seconds and try again
-!search_waiter(W)
    <-  .print("No waiter found");
        .wait(4000);
        !search_waiter(W).

// Get a waiter in P position
+!waiter(P, I, [HEAD|TAIL], W)
    :   P==I
    <-  W = HEAD.

+!waiter(P,I, [HEAD|TAIL], W)
    :   P\==I
    <-  !waiter(P, I+1, TAIL, W).