vu-exchange
===========

Exchange prototype (Smarkets)
After testing idea - to be rewritten in C

Running
-------

git clone git@github.com:miniway/jeromq.git
cd jeromq;mvn install -DskipTests

* test: mvn test
* run: mvn install; ./target/exchange env/boikoro/app.properties

Debts
-----

* correct stop of mq listeners

[events input(via zeromq) -> events persisting(flat file)]{multiple workers} -> [disruptor] -> [business processor]{single thread} -> disruptor -> [output events publishing]{multiple workers}