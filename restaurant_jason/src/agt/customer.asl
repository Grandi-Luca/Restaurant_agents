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
        .print("Checking the menu...");
        .wait(Z*1000);  // check the menu
        .print("After checking the menu, I am ready to order.");
        +order("Spaghetti carbonara");
        !ready_to_order.

+!ready_to_order
    <-  !search_waiter(W);
        .send(W, tell, customer_ready);
        +interlocutor(W).


// Try to find a waiter
+!search_waiter(W)
    :   .print("Searching for available waiters...") &
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

// Normalize random number between X and Y
+!normalize_random(R, Y, Z)
    :   R >= Y
    <-  Z = Y - 1.
+!normalize_random(R, Y, Z)
    :   R < Y
    <-  Z = R.


+waiter_refuse[source(A)]
    :   interlocutor(Other) & 
        A = Other
    <-  -waiter_refuse[source(A)];
        .print("Waiter was busy, I will ask to someone else.");
        !ready_to_order.

+waiter_propose[source(A)]
    :   interlocutor(Other) &
        A = Other
    <-  -waiter_propose[source(A)];
        !send_order(A).

// Send order to waiter
+!send_order(Waiter) 
    :   order(Order)
    <-  .wait(5000); // simulate the time talk to order
        .send(Waiter, tell, take_order(Order));
        -interlocutor(Waiter).

+receive_order(Order) <-
    .print("Order received: ", Order);
    .wait(10000);
    .print("finished eating. I will ask for the bill.");
    +interlocutor(reception);
    !request_bill.

+!request_bill : interlocutor(Other) & Other = reception & order(O) <-
    .send(Other, tell, bill(O)).

+receive_bill(Bill) <-
    .print("Bill received: ", Bill);
    .print("Leaving the restaurant.").