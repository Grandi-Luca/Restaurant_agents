// Agent customer in project 

/* Initial beliefs and rules */
interlocutor(reception).
reply_count(0).

random_int(X,Y,Z) :- .random(R) & Z = math.round((R*Y)+X).

/* Initial goals */
!have_a_seat.


/* Plans */
+!have_a_seat : interlocutor(Other) & Other = reception <- 
    .send(Other, tell, request_table);
    .print("Could i have a seat, please?").

-!have_a_seat <- .print("Receptionist did not respond.").

+refuse_table : interlocutor(Other) & Other = reception <-
    .print("I will wait for a seat");
    -reply_count(I); +reply_count(I+1).

+reply_count(2) <- 
    .print("I will leave.");
    .send(reception, tell, customer_leaving).

+confirm_table 
    :   interlocutor(Other) &   
        Other = reception &
        random_int(1, 15, Z)
    <-  -interlocutor(reception);
        .print("I will have a seat.");
        .wait(Z*1000);  // check the menu
        +order("Spaghetti carbonara");
        !ready_to_order.

+!ready_to_order
    <-  !search_waiter(W);
        .send(W, tell, customer_ready);
        +interlocutor(W).


// Try to find a waiter
+!search_waiter(W)
    :   .print("Searching for waiter") &
        .df_search("waiter", Ws) &
        .length(Ws) > 0 &
        random_int(0, .length(Ws), P)
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

// Normalize random number between X and Y
+!normalize_random(R, Y, Z)
    :   R >= Y
    <-  Z = Y - 1.
+!normalize_random(R, Y, Z)
    :   R < Y
    <-  Z = R.


+waiter_refuse[source(A)]
    :   interlocutor(Other) & 
        provider(A,Other)
    <-  !ready_to_order.

+waiter_propose[source(A)]
    :   interlocutor(Other) &
        provider(A,Other)
    <-  !send_order(A).

// Send order to waiter
+!send_order(Waiter) 
    :   order(Order)
    <-  .send(Waiter, tell, take_order(Order));
        .print("Order taken: ", Order);
        -interlocutor(Waiter).
    
+recive_order(Order) <-
    .print("Order recived: ", Order);
    .wait(10000);
    +interlocutor(reception);
    !request_bill.

+!request_bill : interlocutor(Other) & Other = reception & order(O) <-
    .print("Pay the bill.");
    .send(Other, tell, bill(O)).

+receive_bill(Bill) <-
    .print("Bill received: ", Bill);
    .wait(5000);
    .print("Leaving the restaurant.").
    